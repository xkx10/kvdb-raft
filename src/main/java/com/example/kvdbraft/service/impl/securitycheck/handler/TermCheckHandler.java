package com.example.kvdbraft.service.impl.securitycheck.handler;

import com.example.kvdbraft.annotation.SecurityCheck;
import com.example.kvdbraft.enums.ESecurityCheckType;
import com.example.kvdbraft.po.SecurityCheckContext;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.service.impl.securitycheck.SecurityCheckHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
@SecurityCheck(types = {ESecurityCheckType.Heart,ESecurityCheckType.Vote,ESecurityCheckType.LogAppend})
public class TermCheckHandler implements SecurityCheckHandler {
    @Resource
    PersistenceState persistenceState;
    @Override
    public void handler(SecurityCheckContext context) {
        log.info("starting 任期校验......");
        if(persistenceState.getCurrentTerm() > context.getRpcTerm()){
            log.info("任期校验不通过，my term = {}，rpc term = {}", persistenceState.getCurrentTerm(), context.getRpcTerm());
            throw new SecurityException("任期校验不通过");
        }

    }

}
