package com.example.kvdbraft.service.impl.client;

import java.util.concurrent.ExecutionException;

/**
 * @author WangChao
 * @date 2024-03-04 21:22
 */
public interface OperationStrategy {
    Boolean execute(String command);
}