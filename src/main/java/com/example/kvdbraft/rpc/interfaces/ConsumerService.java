package com.example.kvdbraft.rpc.interfaces;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.vo.Result;

public interface ConsumerService {
    /**
     * 发送超时选举RPC
     * @param url
     * @param requestVoteDTO
     * @return
     */
    Result sendElection(String url, RequestVoteDTO requestVoteDTO);

    /**
     * TODO:接收心跳
     */
}
