package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.enums.ESecurityCheckType;
import com.example.kvdbraft.po.SecurityCheckContext;
import com.example.kvdbraft.service.SecurityCheckService;
import com.example.kvdbraft.service.impl.securitycheck.SecurityCheckProcess;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
public class SecurityCheckServiceImpl implements SecurityCheckService {
    @Resource
    SecurityCheckProcess securityCheckProcess;
    @Override
    public void voteSecurityCheck(RequestVoteDTO requestVoteDTO) {
        SecurityCheckContext context = SecurityCheckContext.builder()
                .rpcTerm(requestVoteDTO.getTerm())
                .rpcLastLogIndex(requestVoteDTO.getLastLogIndex())
                .rpcLastLogTerm(requestVoteDTO.getLastLogTerm())
                .handlerType(ESecurityCheckType.Vote.type)
                .build();
        securityCheckProcess.handler(context);

    }

    @Override
    public void heartSecurityCheck(AppendEntriesDTO appendEntriesDTO) {
        SecurityCheckContext context = SecurityCheckContext.builder()
                .rpcTerm(appendEntriesDTO.getTerm())
                .rpcLastLogIndex(appendEntriesDTO.getPrevLogIndex())
                .rpcLastLogTerm(appendEntriesDTO.getPrevLogTerm())
                .handlerType(ESecurityCheckType.Heart.type)
                .build();
        securityCheckProcess.handler(context);
    }

    @Override
    public void logAppendSecurityCheck(AppendEntriesDTO appendEntriesDTO) {
        SecurityCheckContext context = SecurityCheckContext.builder()
                .rpcTerm(appendEntriesDTO.getTerm())
                .rpcLastLogIndex(appendEntriesDTO.getPrevLogIndex())
                .rpcLastLogTerm(appendEntriesDTO.getPrevLogTerm())
                .handlerType(ESecurityCheckType.LogAppend.type)
                .build();
        securityCheckProcess.handler(context);
    }

}
