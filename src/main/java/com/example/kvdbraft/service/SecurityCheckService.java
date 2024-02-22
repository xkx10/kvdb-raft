package com.example.kvdbraft.service;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.RequestVoteDTO;

public interface SecurityCheckService {
    /**
     * 投票安全检测
     */
    void voteSecurityCheck(RequestVoteDTO requestVoteDTO);

    /**
     * 心跳安全检测
     */
    void heartSecurityCheck(AppendEntriesDTO appendEntriesDTO);
    /**
     * 日志复制安全检测
     */
    void logAppendSecurityCheck(AppendEntriesDTO appendEntriesDTO);
}
