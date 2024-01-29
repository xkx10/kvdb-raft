package com.example.kvdbraft.aop;

import com.example.kvdbraft.rpc.consumer.ConsumerServiceImpl;
import com.example.kvdbraft.rpc.interfaces.ProviderService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CommonProcessingAspect {


    @Before("execution(* com.example.kvdbraft.rpc.consumer.ConsumerServiceImpl.*(..))")
    public void beforeMethodExecution(JoinPoint joinPoint) {

    }

}
