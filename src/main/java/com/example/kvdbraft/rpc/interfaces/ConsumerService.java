package com.example.kvdbraft.rpc.interfaces;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;
import com.example.kvdbraft.vo.Result;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;

public interface ConsumerService {

    /**
     * 发送超时选举RPC
     *
     * @param url            请求rpc地址
     * @param requestVoteDTO 请求体
     * @return 请求结果
     */
    Result<RequestVoteResponseDTO> sendElection(String url, RequestVoteDTO requestVoteDTO);

    /**
     * 发送心跳PRC
     *
     * @param url              请求rpc地址
     * @param appendEntriesDTO 请求体
     * @return 请求结果
     */
    Result<AppendEntriesResponseDTO> sendHeart(String url, AppendEntriesDTO appendEntriesDTO);

    /**
     * 发送日志PRC
     *
     * @param url              请求rpc地址
     * @param appendEntriesDTO 请求体
     * @return 请求结果
     */
    Result<AppendEntriesResponseDTO> sendLog(String url, AppendEntriesDTO appendEntriesDTO);

    /**
     * 调用主节点的写日志功能
     * @param url 主节点地址
     * @param command redis写命令
     */
    Result<Boolean> writeLeader(String url, String command);
}
