package com.example.kvdbraft.service.impl.client.handler;

import com.example.kvdbraft.annotation.ReadOperation;
import com.example.kvdbraft.service.impl.client.OperationStrategy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * @author WangChao
 * @date 2024-03-04 21:23
 */
@ReadOperation
@Service
public class ReadStrategy implements OperationStrategy {

    /**
     * 读请求
     */
    @Override
    public Boolean execute(String command) {
        return true;
    }
}
