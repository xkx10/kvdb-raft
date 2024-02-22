package com.example.kvdbraft.service.impl.securitycheck.handler;

import com.example.kvdbraft.annotation.SecurityCheck;
import com.example.kvdbraft.enums.ESecurityCheckType;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.SecurityCheckContext;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.service.impl.securitycheck.SecurityCheckHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
@SecurityCheck(types = {ESecurityCheckType.LogAppend, ESecurityCheckType.Vote})
public class LogCheckHandler implements SecurityCheckHandler {
    @Resource
    PersistenceState persistenceState;

    @Override
    public void handler(SecurityCheckContext context) {
        log.info("starting 日志校验......");
        int lastIndex = persistenceState.getLogs().size() - 1;
        Log lastLog = persistenceState.getLogs().get(lastIndex);
        if (lastLog.getTerm() > context.getRpcLastLogTerm() || lastLog.getIndex() > context.getRpcLastLogIndex()) {
            log.info("日志校验不通过, my last log term = {}, my last log index = {}, rpc last log term = {}," +
                    " rpc last log index = {},  rpc last log = {}",
                    lastLog.getTerm(), lastLog.getIndex(), context.getRpcLastLogTerm(), context.getRpcLastLogIndex());
            throw new SecurityException("日志校验不通过");
        }

    }

}
