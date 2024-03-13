package com.example.kvdbraft.service.impl.securitycheck.handler;

import com.example.kvdbraft.annotation.SecurityCheck;
import com.example.kvdbraft.enums.ESecurityCheckType;
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
@SecurityCheck(types = {ESecurityCheckType.Heart, ESecurityCheckType.Vote, ESecurityCheckType.LogAppend})
public class ShutdownStatusCheckHandler implements SecurityCheckHandler {
    @Resource
    VolatileState volatileState;
    @Override
    public void handler(SecurityCheckContext context) {
        log.info("starting Shutdown身份校验......");
        if(volatileState.getStatus() == EStatus.Shutdown.status){
            log.info("Shutdown 身份校验成功 status = {}", volatileState.getStatus());
            throw new SecurityException("Shutdown 身份校验成功，该节点处于下线状态");
        }
    }

}
