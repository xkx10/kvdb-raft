package com.example.kvdbraft.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.kvdbraft.annotation.ReadOperation;
import com.example.kvdbraft.annotation.WriteOperation;
import com.example.kvdbraft.service.ClientOperationService;
import com.example.kvdbraft.service.impl.client.OperationStrategy;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author WangChao
 * @date 2024-03-04 21:09
 */
@Service
public class ClientOperationServiceImpl implements ClientOperationService {
    @Resource
    private List<OperationStrategy> strategies;

    @Override
    public <T> T execute(String command) {
        Optional<OperationStrategy> readStrategy = strategies.stream()
                .filter(s -> isRead(command)
                        && s.getClass().isAnnotationPresent(ReadOperation.class))
                .findFirst();
        Optional<OperationStrategy> writeStrategy = strategies.stream()
                .filter(s -> isWrite(command)
                        && s.getClass().isAnnotationPresent(WriteOperation.class))
                .findFirst();
        if (readStrategy.isPresent()) {
            return readStrategy.get().execute(command);
        } else if (writeStrategy.isPresent()) {
            return writeStrategy.get().execute(command);
        } else {
            // 两种都不是，暂定为非法请求
            throw new IllegalArgumentException("Invalid command");
        }
    }

    private boolean isWrite(String command) {
        return StrUtil.startWithAnyIgnoreCase(command, "SET", "DEL", "INCR",
                "DECR", "HSET", "LPUSH", "RPUSH", "SADD", "ZADD", "LINSERT", "HDEL", "SREM", "ZREM",
                "BLRPOP", "BRPOP", "BLPOP", "BRPOP");
    }

    private boolean isRead(String command) {
        return StrUtil.startWithAnyIgnoreCase(command, "GET", "GETRANGE", "LRANGE",
                "LINDEX", "SMEMBERS", "SISMEMBER", "ZRANGE", "ZSCORE", "HGETALL", "HGET",
                "KEYS");
    }
}
