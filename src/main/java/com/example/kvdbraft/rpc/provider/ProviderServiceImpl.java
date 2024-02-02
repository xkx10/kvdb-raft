package com.example.kvdbraft.rpc.provider;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.interfaces.ProviderService;
import com.example.kvdbraft.vo.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

@DubboService(version = "1.0")
@Slf4j
public class ProviderServiceImpl implements ProviderService {
    public final ReentrantLock voteLock = new ReentrantLock();
    @Resource
    PersistenceState persistenceState;
    @Resource
    VolatileState volatileState;
    @Resource
    Cluster cluster;
    public final ReentrantLock heartLock = new ReentrantLock();

    @Override
    public Result<RequestVoteResponseDTO> handlerElection(RequestVoteDTO requestVoteDTO) {
        try {
            if (!voteLock.tryLock()) {
                return Result.failure("获取voteLock锁失败");
            }
            // todo 安全性校验
            // 接收到投票请求就将自己的票投的节点
            volatileState.setStatus(EStatus.Leader.status);
            volatileState.setLeaderId(requestVoteDTO.getCandidateId());
            persistenceState.setCurrentTerm(requestVoteDTO.getTerm());
            persistenceState.setVotedFor(requestVoteDTO.getCandidateId());
            RequestVoteResponseDTO requestVoteResponseDTO = new RequestVoteResponseDTO();
            requestVoteResponseDTO.setVoteGranted(true);
            requestVoteResponseDTO.setTerm(requestVoteDTO.getTerm());
            log.info("vote success {} -> {}", cluster.getId(), requestVoteDTO.getCandidateId());
            return Result.success(requestVoteResponseDTO);
        } catch (Exception e) {
            log.error("接收投票选举异常,requestVoteDTO = {}", requestVoteDTO, e);
            return Result.failure("选举异常");
        } finally {
            voteLock.unlock();
        }
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

    @Override
    public AppendEntriesResponseDTO appendEntries(AppendEntriesDTO EntriesDTO) {
        return null;
    }
}
