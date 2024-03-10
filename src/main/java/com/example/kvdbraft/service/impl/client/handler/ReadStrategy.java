package com.example.kvdbraft.service.impl.client.handler;

import com.example.kvdbraft.annotation.ReadOperation;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.service.StateMachineService;
import com.example.kvdbraft.service.impl.RedisStateMachineServiceImpl;
import com.example.kvdbraft.service.impl.client.OperationStrategy;
import com.example.kvdbraft.service.impl.redis.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author WangChao
 * @date 2024-03-04 21:23
 */
@ReadOperation
@Service
@Slf4j
public class ReadStrategy implements OperationStrategy {

    @Resource
    VolatileState volatileState;
    @Resource
    StateMachineService stateMachineService;
    @Resource
    RedisClient redisClient;

    /**
     * 读请求
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(String command) {
        if (volatileState.getCommitIndex() > volatileState.getLastApplied()) {
            stateMachineService.apply();
        }
        String[] split = command.split(" ");
        return (T) redisClient.get(split[1]);
    }
}
