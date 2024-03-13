package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.NodeConfigField;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.service.HeartbeatService;
import com.example.kvdbraft.service.SecurityCheckService;
import com.example.kvdbraft.service.TriggerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    @Resource
    private ConsumerService consumerService;
    @Resource
    private TriggerService triggerService;
    @Resource
    private SecurityCheckService securityCheckService;

    @Override
    public List<Future<Boolean>> heartNotJudgeResult() {
        //log.info("staring send heart, cluster = {}", cluster);
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
        nodeConfigField.setLastShortLeaseTerm(System.currentTimeMillis());
        return futureArrayList;
    }

    private boolean sendHeart(String url) {
        try {
            // 心跳只关心 term 和 leaderID
            AppendEntriesDTO heartDTO = AppendEntriesDTO.builder()
                    .entries(null)
                    .leaderId(cluster.getId())
                    .term(persistenceState.getCurrentTerm())
                    // 记录自己的CommitIndex，如果是leader，则为leaderCommit
                    .leaderCommit(volatileState.getCommitIndex())
                    .build();
            AppendEntriesResponseDTO heartResult = consumerService.sendHeart(url, heartDTO).getData();
            long term = heartResult.getTerm();
            if (term > persistenceState.getCurrentTerm()) {
                log.error("self will become follower, he's term : {}, my term : {}", term, persistenceState.getCurrentTerm());
                persistenceState.setCurrentTerm(term);
                persistenceState.setVotedFor(null);
                volatileState.setStatus(EStatus.Follower.status);
                // 开始超时选举任务，停止心跳任务
                triggerService.startElectionTask();
                triggerService.stopHeartTask();
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
        long currentTerm = persistenceState.getCurrentTerm();
        try {
            securityCheckService.heartSecurityCheck(heartDTO);
        }catch (SecurityException securityException){
            log.error("心跳校验不通过 message = {}", securityException.getMessage());
            return AppendEntriesResponseDTO.builder()
                    .term(currentTerm).success(false).build();
        }
        // 更新超时选举时间
        triggerService.updateElectionTaskTime();
        triggerService.stopHeartTask();
        // 设置当前节点的leader地址
        volatileState.setLeaderId(heartDTO.getLeaderId());
        volatileState.setStatus(EStatus.Follower.status);
        persistenceState.setCurrentTerm(heartDTO.getTerm());
        persistenceState.setVotedFor(null);
        //心跳 只apply当前节点没写入状态机的日志
//            log.info("node {} append heartbeat success , he's term : {}, my term : {}",
//                    heartDTO.getLeaderId(), heartDTO.getTerm(), heartDTO.getTerm());
        //如果 leaderCommit > commitIndex，令 commitIndex 等于 leaderCommit 和 新日志条目索引值中较小的一个
        if (heartDTO.getLeaderCommit() > volatileState.getCommitIndex()) {
            // 和日志集合最后一条记录比较，最后一条记录可能未提交
            int commitIndex = Math.min(heartDTO.getLeaderCommit(), volatileState.getLastIndex());
            volatileState.setCommitIndex(commitIndex);
        }
        return AppendEntriesResponseDTO.builder()
                .term(persistenceState.getCurrentTerm())
                .success(true).build();
    }
}
