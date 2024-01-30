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

    RequestVoteResponseDTO acceptElection(RequestVoteDTO requestVoteDTO);



}
