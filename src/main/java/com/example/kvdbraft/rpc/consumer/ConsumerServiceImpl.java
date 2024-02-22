package com.example.kvdbraft.rpc.consumer;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;
import com.example.kvdbraft.rpc.factory.ReferenceFactory;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.rpc.interfaces.ProviderService;
import com.example.kvdbraft.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConsumerServiceImpl implements ConsumerService {
    // TODO:上次商谈结果，是想用延迟队列实现超时效果

    @Override
    public Result<RequestVoteResponseDTO> sendElection(String url, RequestVoteDTO requestVoteDTO) {
        try {
            // TODO：测试一下
            ProviderService providerService = ReferenceFactory.getOrCreateReference(url);
            // 开启远程调用
            return providerService.handlerElection(requestVoteDTO);
        } catch (RpcException rpcException) {
            // RPC连接创建后发送请求出现异常（一般情况是对面机器宕机了）
            return Result.failure("RPC请求失败：" + rpcException.getMessage());
        } catch (Exception e) {
            // 其他异常
            return Result.failure("发生未知错误：" + e.getMessage());
        }
    }

    @Override
    public Result<AppendEntriesResponseDTO> sendHeart(String url, AppendEntriesDTO appendEntriesDTO) {
        try {
            ProviderService providerService = ReferenceFactory.getOrCreateReference(url);
            return providerService.handlerHeart(appendEntriesDTO);
        } catch (RpcException rpcException) {
            return Result.failure("RPC请求失败：" + rpcException.getMessage());
        } catch (Exception e) {
            return Result.failure("发生未知错误：" + e.getMessage());
        }
    }
}