package com.example.kvdbraft.rpc.provider;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.rpc.interfaces.ProviderService;
import com.example.kvdbraft.vo.Result;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService(version = "1.0")
public class ProviderServiceImpl implements ProviderService {
    @Override
    public Result handlerElection(RequestVoteDTO requestVoteDTO) {
        System.out.println(requestVoteDTO);
        return Result.success(requestVoteDTO);
    }
}
