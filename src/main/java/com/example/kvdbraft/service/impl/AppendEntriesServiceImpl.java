package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.NodeConfigField;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.LeaderVolatileState;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.service.AppendEntriesService;
import com.example.kvdbraft.service.LogService;
import com.example.kvdbraft.service.RocksService;
import com.example.kvdbraft.service.SecurityCheckService;
import com.example.kvdbraft.service.TriggerService;
import com.example.kvdbraft.vo.Result;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author WangChao
 * @date 2024-02-22 19:51
 */
@Service
@Log4j2
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

    private int oneRpcTimeOut = 1000;

    private final ReentrantLock logLock = new ReentrantLock();

    // 客户端调用发送日志时，如果失败，则nextIndex-1后重新调用该方法
    @Override
    public List<Future<Boolean>> sendLogToFollow() {
        // 发送给所有节点日志
        if (volatileState.getStatus() != EStatus.Leader.status) {
            return null;
        }
       
        Set<String> follows = cluster.getNoMyselfClusterIds();
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
            Log preLog = persistenceState.getLogs().get(index - 1);
            List<Log> entries = persistenceState.getLogs().subList(index, persistenceState.getLogs().size());
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
                preLog = persistenceState.getLogs().get(index - 1);
                entries = persistenceState.getLogs().subList(index, persistenceState.getLogs().size());
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
            if(!logResult.getSuccess() && persistenceState.getCurrentTerm() < logResult.getTerm()){
                volatileState.setStatus(EStatus.Follower.status);
                triggerService.stopHeartTask();
                triggerService.startElectionTask();
            }
            nextIndexMap.put(url, persistenceState.getLogs().size());
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
            securityCheckService.logAppendSecurityCheck(entriesDTO);
            volatileState.setLeaderId(entriesDTO.getLeaderId());
            volatileState.setStatus(EStatus.Follower.status);
            persistenceState.setCurrentTerm(entriesDTO.getTerm());
            persistenceState.setVotedFor(null);
            // 确保preLog一致性，上一个没冲突继续
            Log prelog = logService.getLastLog();
            if(!prelog.getIndex().equals(entriesDTO.getPrevLogIndex()) || !prelog.getTerm().equals(entriesDTO.getPrevLogTerm())){
                return AppendEntriesResponseDTO.builder()
                        .term(currentTerm).success(false).build();
            }
            // 如果已经存在的日志条目和新的产生冲突，删除这一条和之后所有的
            if (entriesDTO.getPrevLogIndex() + 1 < persistenceState.getLogs().size()) {
                Log existLog = persistenceState.getLogs().get(entriesDTO.getPrevLogIndex() + 1);
                if (existLog != null) {
                    // 删除这一条和之后所有的,
                    logService.removeOnStartIndex(entriesDTO.getPrevLogIndex() + 1);
                }
            }
            // 写日志
            logService.writeLog(entriesDTO.getEntries());
            volatileState.setLastIndex(volatileState.getLastIndex() + entriesDTO.getEntries().size());
            log.info("接受日志成功，time = {}",System.currentTimeMillis() - start);
            return AppendEntriesResponseDTO.builder()
                    .term(currentTerm)
                    .success(true).build();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            // 主节点下线
            return AppendEntriesResponseDTO.builder()
                    .term(persistenceState.getCurrentTerm()).success(false).build();
        }finally {
            logLock.unlock();
        }
    }
}
