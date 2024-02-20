package com.example.kvdbraft.rpc.interfaces;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;
import com.example.kvdbraft.vo.Result;

import java.util.List;
import java.util.concurrent.Future;

public interface ConsumerService {

    /**
     * 发送超时选举RPC
     * @param url 请求rpc地址
     * @param requestVoteDTO 请求体
     * @return 请求结果
     */
    Result<RequestVoteResponseDTO> sendElection(String url, RequestVoteDTO requestVoteDTO);

    /**
     * 发送心跳
     * @return 跟随者回应记录
     */
    List<Future<Boolean>> heartNotJudgeResult();
}
