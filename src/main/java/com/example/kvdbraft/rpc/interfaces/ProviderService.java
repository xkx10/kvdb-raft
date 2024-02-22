package com.example.kvdbraft.rpc.interfaces;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;
import com.example.kvdbraft.vo.Result;

import java.util.List;
import java.util.concurrent.Future;

public interface ProviderService {
    /**
     * 接收超时选举RPC
     *
     * @param requestVoteDTO 请求投票体
     * @return 投票回应
     */
    Result<RequestVoteResponseDTO> handlerElection(RequestVoteDTO requestVoteDTO);

    /**
     * 接收心跳RPC
     * @param heartDTO
     * @return 从节点接收结果集合
     */
    AppendEntriesResponseDTO handlerHeart(AppendEntriesDTO heartDTO);

    /**
     * 跟随节点日志复制RPC
     * @param EntriesDTO
     * @return 从节点接收结果集合
     */
    AppendEntriesResponseDTO appendEntries(AppendEntriesDTO EntriesDTO);
}
