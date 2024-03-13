package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.enums.EPersistenceKeys;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.Command;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.NodeConfigField;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.LeaderVolatileState;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.service.AppendEntriesService;
import com.example.kvdbraft.service.ClusterService;
import com.example.kvdbraft.service.LogService;
import com.example.kvdbraft.service.SecurityCheckService;
import com.example.kvdbraft.service.TriggerService;
import com.example.kvdbraft.service.impl.redis.RedisClient;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author WangChao
 * @date 2024-02-22 19:51
 */
@Service
@Slf4j
public class AppendEntriesServiceImpl implements AppendEntriesService {
    @Resource
    private ConsumerService consumerService;
    @Resource
    private SecurityCheckService securityCheckService;
    @Resource
    private VolatileState volatileState;
    @Resource
    private Cluster cluster;
    ExecutorService logExecutor = Executors.newScheduledThreadPool(5);

    @Resource
    private NodeConfigField nodeConfigField;

    @Resource
    private LeaderVolatileState leaderVolatileState;
    @Resource
    private PersistenceState persistenceState;
    @Resource
    private LogService logService;
    @Resource
    private TriggerService triggerService;
    @Resource
    private RedisClient redisClient;
    @Resource
    private ClusterService clusterService;
    private int oneRpcTimeOut = 1000;
    private int sumRpcTimeOut = 1500;

    private final ReentrantLock logLock = new ReentrantLock();
    private final ReentrantLock sendLogLock = new ReentrantLock();

    // 客户端调用发送日志时，如果失败，则nextIndex-1后重新调用该方法
    @Override
    public Boolean sendLogToFollow(Log sendLog) {
        try {
            if (!sendLogLock.tryLock(1, TimeUnit.SECONDS)) {
                log.info("appendLock锁获取失败");
                return Boolean.valueOf(false);
            }
            // 如果不是主节点，则进行rpc调用主节点的写日志请求
            if (volatileState.getStatus() != EStatus.Leader.status) {
                boolean rs = consumerService.writeLeader(volatileState.getLeaderId(), sendLog).getData();
                return Boolean.valueOf(rs);
            }
            // TODO:先假设command为set key value的形式

            int lastIndex = volatileState.getLastIndex() + 1;
            sendLog.setTerm(persistenceState.getCurrentTerm());
            sendLog.setIndex(lastIndex);
            redisClient.lSetByShard(EPersistenceKeys.LogEntries.key, sendLog, lastIndex);
            volatileState.setLastIndex(lastIndex);
            long start = System.currentTimeMillis();
            Boolean sendLogResult;
            if (!cluster.isChangeStatus()) {
                sendLogResult = getSendLogResult(cluster.getClusterIds());
            } else {
                sendLogResult = getSendLogResult(cluster.getNoMyselfOldClusterIds()) &&
                        getSendLogResult(cluster.getNoMyselfNewClusterIds());
            }
            if (!sendLogResult) {
                log.info("日志写入失败");
                return Boolean.valueOf(false);
            }

            long end = System.currentTimeMillis();
            // 写入成功了，更新commitIndex和应用到状态机，并且将状态进行持久化到rocks
            volatileState.setCommitIndex(lastIndex);
            log.info("日志写入成功 time = {} ", end - start);
            return Boolean.valueOf(true);
        } catch (InterruptedException e) {
            log.error("线程异常中断 message = {}", e.getMessage(), e);
            return Boolean.valueOf(false);
        } finally {
            sendLogLock.unlock();
        }
    }
    private Boolean getSendLogResult(Set<String> clusterIds) throws InterruptedException {
        int lastIndex = volatileState.getLastIndex() + 1;
        List<Future<Boolean>> futures = sendLogToFollow(clusterIds);
        AtomicInteger success = new AtomicInteger(1);
        CountDownLatch latch = new CountDownLatch(futures.size());
        for (Future<Boolean> future : futures) {
            logExecutor.submit(() -> {
                try {
                    Boolean res = future.get(oneRpcTimeOut, TimeUnit.MILLISECONDS);
                    if (res) {
                        success.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("从节点日志异步同步错误");
                } finally {
                    //  每次调用这个方法，计数器的值就会减一。当计数器的值减到零时，所有因为调用 await() 方法而在等待的线程都会被唤醒
                    latch.countDown();
                }
            });
        }
        // 等待三秒同步
        boolean await = latch.await(sumRpcTimeOut, TimeUnit.MILLISECONDS);
        if (!await) {
            log.error("存在一个或多个节点连接超时");
        }
        if (success.get() * 2 <= clusterIds.size()) {
            // 回退日志和lastIndex
            redisClient.lRemoveLastShard(EPersistenceKeys.LogEntries.key, lastIndex);
            volatileState.setLastIndex(lastIndex - 1);
            return Boolean.valueOf(false);
        }
        return Boolean.valueOf(true);
    }


    private List<Future<Boolean>> sendLogToFollow(Set<String> follows) {
        // 发送给所有节点日志
        if (volatileState.getStatus() != EStatus.Leader.status) {
            return null;
        }
        List<Future<Boolean>> futureArrayList = new ArrayList<>();
        // 拿到其他节点的地址，除了自己的调用地址
        for (String peer : follows) {
            // 提交一个任务
            futureArrayList.add(logExecutor.submit(() -> sendLog(peer)));
        }
        nodeConfigField.setLastShortLeaseTerm(System.currentTimeMillis());
        return futureArrayList;
    }

    private boolean sendLog(String url) {
        try {
            // 根据url拿到nextIndex，然后将其后面的日志发送过去
            Map<String, Integer> nextIndexMap = leaderVolatileState.getNextIndexMap();
            Integer index = nextIndexMap.get(url);
            Log preLog = (Log) redisClient.lGetByShardIndex(EPersistenceKeys.LogEntries.key, index - 1);
            List<Log> entries =
                    redisClient.lGetByShardIndexAfter(EPersistenceKeys.LogEntries.key, index)
                            .stream().map(obj -> (Log) obj)
                            .collect(Collectors.toList());
            AppendEntriesDTO logDTO = AppendEntriesDTO.builder()
                    .entries(entries)
                    .prevLogIndex(preLog.getIndex())
                    .prevLogTerm(preLog.getTerm())
                    .leaderId(cluster.getId())
                    .term(persistenceState.getCurrentTerm())
                    // 记录自己的CommitIndex，如果是leader，则为leaderCommit
                    .leaderCommit(volatileState.getCommitIndex())
                    .build();
            AppendEntriesResponseDTO logResult = consumerService.sendLog(url, logDTO).getData();
            // 同步失败则进行重试
            while (!logResult.getSuccess() && persistenceState.getCurrentTerm() >= logResult.getTerm()) {
                index--;
                preLog = (Log) redisClient.lGetByShardIndex(EPersistenceKeys.LogEntries.key, index - 1);
                entries = redisClient.lGetByShardIndexAfter(EPersistenceKeys.LogEntries.key, index)
                        .stream().map(obj -> (Log) obj)
                        .collect(Collectors.toList());
                logDTO = AppendEntriesDTO.builder()
                        .entries(entries)
                        .prevLogIndex(preLog.getIndex())
                        .prevLogTerm(preLog.getTerm())
                        .leaderId(cluster.getId())
                        .term(persistenceState.getCurrentTerm())
                        // 记录自己的CommitIndex，如果是leader，则为leaderCommit
                        .leaderCommit(volatileState.getCommitIndex())
                        .build();
                logResult = consumerService.sendLog(url, logDTO).getData();
            }
            // TODO:这里有问题，不该是主节点发起选举，而是从节点开始选举
            if (!logResult.getSuccess() && persistenceState.getCurrentTerm() < logResult.getTerm()) {
                volatileState.setStatus(EStatus.Follower.status);
                triggerService.stopHeartTask();
                triggerService.startElectionTask();
                return false;
            }
            nextIndexMap.put(url, index + 1);
            return logResult.getSuccess();
        } catch (Exception e) {
            log.error("HeartBeatTask RPC Fail, request URL : {}", url, e);
            return false;
        }
    }

    @Override
    public AppendEntriesResponseDTO appendEntries(AppendEntriesDTO entriesDTO) {
        try {
            if (!logLock.tryLock(oneRpcTimeOut, TimeUnit.MILLISECONDS)) {
                log.info("appendLock锁获取失败");
                return AppendEntriesResponseDTO.fail();
            }
            long start = System.currentTimeMillis();
            long currentTerm = persistenceState.getCurrentTerm();
            try {
                securityCheckService.logAppendSecurityCheck(entriesDTO);
            }catch (SecurityException securityException){
                log.error("日志校验不通过 message = {}", securityException.getMessage());
                return AppendEntriesResponseDTO.builder()
                        .term(currentTerm).success(false).build();
            }
            volatileState.setLeaderId(entriesDTO.getLeaderId());
            volatileState.setStatus(EStatus.Follower.status);
            persistenceState.setCurrentTerm(entriesDTO.getTerm());
            persistenceState.setVotedFor(null);
            // 确保preLog一致性，上一个没冲突继续
            Log prelog = logService.getLastLog();
            if (!prelog.getIndex().equals(entriesDTO.getPrevLogIndex()) || !prelog.getTerm().equals(entriesDTO.getPrevLogTerm())) {
                return AppendEntriesResponseDTO.builder()
                        .term(currentTerm).success(false).build();
            }
            log.error("日志删除前，日志实体为 = {}", entriesDTO);
            // 如果已经存在的日志条目和新的产生冲突，删除这一条和之后所有的
            if (entriesDTO.getPrevLogIndex() + 1 <= volatileState.getLastIndex()) {
                // 删除这一条和之后所有的记录
                logService.removeOnStartIndex(entriesDTO.getPrevLogIndex() + 1);
                log.error("日志删除后，日志实体为 = {}", entriesDTO);
            }
            // 写日志
            logService.writeLog(entriesDTO.getEntries());
            for (Log entry : entriesDTO.getEntries()) {
                if(entry.getCluster() != null){
                    cluster.setChangeStatus(entry.getCluster().isChangeStatus());
                    cluster.setNewClusterIds(entry.getCluster().getNewClusterIds());
                    cluster.setOldClusterIds(entry.getCluster().getOldClusterIds());
                    cluster.setClusterIds(entry.getCluster().getClusterIds());
                }
            }
            clusterService.clusterSelfCheckAndShutdown();
            volatileState.setLastIndex(volatileState.getLastIndex() + entriesDTO.getEntries().size()-1);
            log.info("接受日志成功，time = {}", System.currentTimeMillis() - start);
            return AppendEntriesResponseDTO.builder()
                    .term(currentTerm)
                    .success(true).build();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            // 主节点下线
            return AppendEntriesResponseDTO.builder()
                    .term(persistenceState.getCurrentTerm()).success(false).build();
        } finally {
            logLock.unlock();
        }
    }
}
