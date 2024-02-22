package com.example.kvdbraft.service.impl.securitycheck.handler;

import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.SecurityCheckContext;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.service.impl.securitycheck.SecurityCheckHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class LeaderStatusCheckHandler implements SecurityCheckHandler {
    @Resource
    VolatileState volatileState;
    @Resource
    Cluster cluster;
    @Override
    public void handler(SecurityCheckContext context) {
        log.info("starting leader身份校验......");
        if(volatileState.getStatus() != EStatus.Leader.status){
            log.info("leader 身份校验失败，node = {}，status = {}", cluster.getId(), volatileState.getStatus());
            throw new SecurityException("leader身份校验失败");
        }

    }

}
