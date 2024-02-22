package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.NodeConfigField;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.factory.ReferenceFactory;
import com.example.kvdbraft.rpc.interfaces.ProviderService;
import com.example.kvdbraft.service.HeartbeatService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author WangChao
 * @date 2024-02-04 9:11
 */
@Service
@Slf4j
public class HeartbeatServiceImpl implements HeartbeatService {
    ExecutorService heartExecutor = Executors.newScheduledThreadPool(5);

    @Resource
    private VolatileState volatileState;

    @Resource
    private Cluster cluster;

    @Resource
    private PersistenceState persistenceState;

    @Resource
    private NodeConfigField nodeConfigField;

    private final ReentrantLock heartLock = new ReentrantLock();

    @Override
    public List<Future<Boolean>> heartNotJudgeResult() {
        // 发送给所有节点心跳，所有节点处理心跳后返回
        if (volatileState.getStatus() != EStatus.Leader.status) {
            return null;
        }
        Set<String> follows = cluster.getNoMyselfClusterIds();
        List<Future<Boolean>> futureArrayList = new ArrayList<>();
        // 拿到其他节点的地址，除了自己的调用地址
        for (String peer : follows) {
            // 提交一个任务
            futureArrayList.add(heartExecutor.submit(() -> sendHeart(peer)));
        }
        return futureArrayList;
    }

    private boolean sendHeart(String url) {
        try {
            ProviderService providerService = ReferenceFactory.getOrCreateReference(url);
            nodeConfigField.setLastShortLeaseTerm(System.currentTimeMillis());
            // 心跳只关心 term 和 leaderID
            AppendEntriesDTO heartDTO = AppendEntriesDTO.builder()
                    .entries(null)
                    .leaderId(cluster.getId())
                    .term(persistenceState.getCurrentTerm())
                    // 记录自己的CommitIndex，如果是leader，则为leaderCommit
                    .leaderCommit(volatileState.getCommitIndex())
                    .build();
            AppendEntriesResponseDTO heartResult = providerService.handlerHeart(heartDTO).getData();
            long term = heartResult.getTerm();
            if (term > persistenceState.getCurrentTerm()) {
                log.error("self will become follower, he's term : {}, my term : {}", term, persistenceState.getCurrentTerm());
                persistenceState.setCurrentTerm(term);
                persistenceState.setVotedFor(null);
                volatileState.setStatus(EStatus.Follower.status);
                // 刷新上次选举时间
                // preElectionTime = System.currentTimeMillis();
                return false;
            }
        } catch (Exception e) {
            log.error("HeartBeatTask RPC Fail, request URL : {} ", url);
            return false;
        }
        return true;
    }

    @Override
    public AppendEntriesResponseDTO handlerHeart(AppendEntriesDTO heartDTO) {
        AppendEntriesResponseDTO result = AppendEntriesResponseDTO.fail();
        if (!heartLock.tryLock()) {
            log.info("heartLock锁获取失败");
            return result;
        }
        try {
            long currentTerm = persistenceState.getCurrentTerm();
            if (heartDTO.getTerm() < currentTerm) {
                log.info("对方term小于自己");
                return AppendEntriesResponseDTO.builder()
                        .term(currentTerm).success(false).build();
            }
            // TODO：刷新选举时间和心跳时间，看是否需要
            // node.setPreElectionTime(System.currentTimeMillis());
            // node.setPreHeartBeatTime(System.currentTimeMillis());
            // 设置当前节点的leader地址
            volatileState.setLeaderId(heartDTO.getLeaderId());
            volatileState.setStatus(EStatus.Follower.status);
            persistenceState.setCurrentTerm(heartDTO.getTerm());
            persistenceState.setVotedFor(null);
            //心跳 只apply当前节点没写入状态机的日志
            log.info("node {} append heartbeat success , he's term : {}, my term : {}",
                    heartDTO.getLeaderId(), heartDTO.getTerm(), heartDTO.getTerm());
            // 下一个需要提交的日志的索引（如有）
            long nextCommit = volatileState.getCommitIndex() + 1;
            //如果 leaderCommit > commitIndex，令 commitIndex 等于 leaderCommit 和 新日志条目索引值中较小的一个
            if (heartDTO.getLeaderCommit() > volatileState.getCommitIndex()) {
                // 和日志集合最后一条记录比较，最后一条记录可能未提交
                int commitIndex = Math.min(heartDTO.getLeaderCommit(), volatileState.getLastIndex());
                volatileState.setCommitIndex(commitIndex);
                // 更新应用到状态机的最小值，我觉得不应该在这里就是设置，应该应用完再加1
                volatileState.setLastApplied(commitIndex);
            }
            while (nextCommit <= volatileState.getCommitIndex()) {
                // 提交之前的日志，并提交到状态机
                // node.getStateMachine().apply(node.getLogModule().read(nextCommit));
                nextCommit++;
            }
            return AppendEntriesResponseDTO.builder()
                    .term(persistenceState.getCurrentTerm())
                    .success(true).build();
        } finally {
            heartLock.unlock();
        }
    }
}
