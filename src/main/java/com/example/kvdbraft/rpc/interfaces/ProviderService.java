package com.example.kvdbraft.rpc.interfaces;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.vo.Result;

public interface ProviderService {
    /**
     * 接收超时选举RPC
     * @param requestVoteDTO
     * @return
     */
    Result handlerElection(RequestVoteDTO requestVoteDTO);

}
