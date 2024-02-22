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
@SecurityCheck(types = {ESecurityCheckType.Vote})
public class RepeatVoteCheckHandler implements SecurityCheckHandler {
    @Resource
    PersistenceState persistenceState;
    @Override
    public void handler(SecurityCheckContext context) {
        log.info("starting 重复投票校验......");
        if(persistenceState.getVotedFor() != null && persistenceState.getCurrentTerm().equals(context.getRpcTerm())){
            log.info("重复投票校验不通过, 节点在term = {} 的时候已投票给了 node = {}", persistenceState.getCurrentTerm(), persistenceState.getVotedFor());
            throw new SecurityException("重复投票校验不通过");
        }

    }

}
