package com.example.kvdbraft.service;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;
import org.springframework.stereotype.Service;


public interface ElectionService {
    /**
     * 发起选举
     * @return 获得投票数
     */
    boolean startElection();

    /**
     * 接收选举投票请求
     * @param requestVoteDTO 请求投票DTO
     * @return 处理请求投票放回的DTO
     */
    RequestVoteResponseDTO acceptElection(RequestVoteDTO requestVoteDTO);

}
