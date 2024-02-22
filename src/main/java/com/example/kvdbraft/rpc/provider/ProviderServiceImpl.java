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
import com.example.kvdbraft.service.AppendEntriesService;
import com.example.kvdbraft.service.HeartbeatService;
import com.example.kvdbraft.service.ElectionService;
import com.example.kvdbraft.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
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

    @Resource
    private HeartbeatService heartbeatService;
    @Resource
    private AppendEntriesService appendEntriesService;
    @Resource
    ElectionService electionService;
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
    public Result<AppendEntriesResponseDTO> handlerHeart(AppendEntriesDTO heartDTO) {
        return Result.success(heartbeatService.handlerHeart(heartDTO));
    }

    @Override
    public Result<AppendEntriesResponseDTO> appendEntries(AppendEntriesDTO entriesDTO) {
        return Result.success(appendEntriesService.appendEntries(entriesDTO));
    }
}
