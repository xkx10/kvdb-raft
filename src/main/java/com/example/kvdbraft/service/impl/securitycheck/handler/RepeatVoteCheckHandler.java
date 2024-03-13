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
        // 当前任期已经投过票
        // !context.getRpcNodeId().equals(persistenceState.getVotedFor()
        // 这个条件主要是考虑到集群变更如果一阶段发生宕机，这个时候会对两个集群中的节点发起选举，如果该节点存在两个集群中，那应该允许给节点多次投票，不算重复投票
        if (persistenceState.getVotedFor() != null && persistenceState.getCurrentTerm().equals(context.getRpcTerm())
                && !context.getRpcNodeId().equals(persistenceState.getVotedFor())) {
            log.info("重复投票校验不通过, 节点在term = {} 的时候已投票给了 node = {}", persistenceState.getCurrentTerm(), persistenceState.getVotedFor());
            throw new SecurityException("重复投票校验不通过");
        }

    }

}
