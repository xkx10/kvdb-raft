package com.example.kvdbraft.service.impl.securitycheck;

import com.example.kvdbraft.annotation.SecurityCheck;
import com.example.kvdbraft.enums.ESecurityCheckType;
import com.example.kvdbraft.po.SecurityCheckContext;

import java.util.Arrays;

public interface SecurityCheckHandler {
    /**
     * 具体处理逻辑
     * @param context 上下文
     */
    void handler(SecurityCheckContext context);

    /**
     * 判断是否能处理handler
     * @param context
     * @return
     */
    default boolean canHandler(SecurityCheckContext context){
        SecurityCheck annotation = getClass().getAnnotation(SecurityCheck.class);
        if(annotation == null){
            return false;
        }
        ESecurityCheckType[] types = annotation.types();
        return Arrays.stream(types).anyMatch(e -> e.type == context.getHandlerType());
    }
}
