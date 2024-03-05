package com.example.kvdbraft.service;

/**
 * @author WangChao
 * @date 2024-03-04 21:09
 */
public interface ClientOperationService {
    Boolean execute(String command);
}
