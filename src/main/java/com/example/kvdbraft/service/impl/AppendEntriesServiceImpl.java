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
import com.example.kvdbraft.service.SecurityCheckService;
import com.example.kvdbraft.vo.Result;
import jakarta.annotation.Resource;
import lombok.extern.log4j.Log4j;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
            List<Log> entries = persistenceState.getLogs().subList(index, persistenceState.getLogs().size());
            // 心跳只关心 term 和 leaderID
            AppendEntriesDTO logDTO = AppendEntriesDTO.builder()
                    .entries(entries)
                    .leaderId(cluster.getId())
                    .term(persistenceState.getCurrentTerm())
                    // 记录自己的CommitIndex，如果是leader，则为leaderCommit
                    .leaderCommit(volatileState.getCommitIndex())
                    .build();
            AppendEntriesResponseDTO logResult = consumerService.sendLog(url, logDTO).getData();
            // 检查日志同步结果
//            long term = logResult.getTerm();
//            if (term > persistenceState.getCurrentTerm()) {
//                log.error("self will become follower, he's term : {}, my term : {}", term, persistenceState.getCurrentTerm());
//                persistenceState.setCurrentTerm(term);
//                persistenceState.setVotedFor(null);
//                volatileState.setStatus(EStatus.Follower.status);
//                // 刷新上次选举时间
//                // preElectionTime = System.currentTimeMillis();
//                return false;
//            }

        } catch (Exception e) {
            log.error("HeartBeatTask RPC Fail, request URL : {} ", url);
            return false;
        }
        return true;
    }

    @Override
    public AppendEntriesResponseDTO appendEntries(AppendEntriesDTO entriesDTO) {
        // 日志应用
        AppendEntriesResponseDTO result = AppendEntriesResponseDTO.fail();
        // 日志安全性校验
        try {
            securityCheckService.logAppendSecurityCheck(entriesDTO);
        } catch (SecurityException e) {
            return result;
        }
        if (!logLock.tryLock()) {
            log.info("appendLock锁获取失败");
            return result;
        }
        try {
            long currentTerm = persistenceState.getCurrentTerm();
            if (entriesDTO.getTerm() < currentTerm) {
                log.info("对方term小于自己");
                return AppendEntriesResponseDTO.builder()
                        .term(currentTerm).success(false).build();
            }
            volatileState.setLeaderId(entriesDTO.getLeaderId());
            volatileState.setStatus(EStatus.Follower.status);
            persistenceState.setCurrentTerm(entriesDTO.getTerm());
            persistenceState.setVotedFor(null);
            // 如果已经存在的日志条目和新的产生冲突（索引值相同但是任期号不同），删除这一条和之后所有的
            Log existLog = persistenceState.getLogs().get(entriesDTO.getPrevLogIndex() + 1);
            if (existLog != null) {
                // 删除这一条和之后所有的, 然后写入日志和更新原有rocks数据库，并更新lastIndex
                logService.removeOnStartIndex(entriesDTO.getPrevLogIndex() + 1);
            }
            // 写日志
            logService.writeLog(entriesDTO.getEntries());
            return AppendEntriesResponseDTO.builder()
                    .term(persistenceState.getCurrentTerm())
                    .success(true).build();
        } finally {
            logLock.unlock();
        }
    }
}
