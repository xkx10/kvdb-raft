package com.example.kvdbraft.service.impl.securitycheck;

import com.example.kvdbraft.po.SecurityCheckContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
@Component
@Slf4j
public class SecurityCheckProcess {
    @Resource
    List<SecurityCheckHandler> securityHandlerList;

    /**
     * 将handler连接成责任链
     * @param context
     */
    public void handler(SecurityCheckContext context){
        for (SecurityCheckHandler securityHandler : securityHandlerList) {
            if(securityHandler.canHandler(context)){
                securityHandler.handler(context);
            }
        }
        log.info("success, 安全性校验通过。");
    }
}

