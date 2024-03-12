package com.example.kvdbraft.rpc.provider;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.rpc.interfaces.ProviderService;
import com.example.kvdbraft.service.AppendEntriesService;
import com.example.kvdbraft.service.ClientOperationService;
import com.example.kvdbraft.service.ClusterService;
import com.example.kvdbraft.service.HeartbeatService;
import com.example.kvdbraft.service.ElectionService;
import com.example.kvdbraft.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;
import java.util.concurrent.locks.ReentrantLock;

@DubboService(version = "1.0")
@Slf4j
public class ProviderServiceImpl implements ProviderService {
    public final ReentrantLock voteLock = new ReentrantLock();
    @Resource
    private HeartbeatService heartbeatService;
    @Resource
    private AppendEntriesService appendEntriesService;
    @Resource
    private ElectionService electionService;
    @Resource
    private ClientOperationService clientOperationService;
    @Resource
    ClusterService clusterService;

    @Override
    public Result<RequestVoteResponseDTO> handlerElection(RequestVoteDTO requestVoteDTO) {
        if (!voteLock.tryLock()) {
            return Result.failure("获取voteLock锁失败");
        }
        try {
            RequestVoteResponseDTO requestVoteResponseDTO = electionService.acceptElection(requestVoteDTO);
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

    @Override
    public Result<Boolean> write(Log sendLog) {
        // 调用写数据服务
        return appendEntriesService.sendLogToFollow(sendLog) ? Result.success(null) : Result.failure(null);
    }



}
