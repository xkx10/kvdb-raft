package com.example.kvdbraft.service.impl.client.handler;

import com.example.kvdbraft.annotation.WriteOperation;
import com.example.kvdbraft.enums.EPersistenceKeys;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.Command;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.service.AppendEntriesService;
import com.example.kvdbraft.service.SecurityCheckService;
import com.example.kvdbraft.service.StateMachineService;
import com.example.kvdbraft.service.impl.client.OperationStrategy;
import com.example.kvdbraft.service.impl.redis.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author WangChao
 * @date 2024-03-04 21:24
 */
@WriteOperation
@Service
@Slf4j
public class WriteStrategy implements OperationStrategy {

    @Resource
    private VolatileState volatileState;
   @Resource
   private AppendEntriesService appendEntriesService;

    /**
     * 1. 如果不是主节点，则进行rpc调用主节点的写日志请求
     * 2. 如果是主节点，则直接进行数据写入，并且发起日志同步请求
     * 3. 封装，写日志的请求（确定哪些参数要改动）
     * 4. 主节点调用日志同步rpc
     * 5. 超过半数以上同步成功，则将状态应用到状态机（这时候失败了怎么办？）
     * 6. 如果没有半数以上，则写入失败，回退log[]，并返回客户端写入错误
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T execute(String command) {
        String[] c = command.split(" ");
        Command cm = Command.builder()
                .key(c[1])
                .value(c[2])
                .build();
        Log send = Log.builder()
                .command(cm)
                .build();
        return (T) appendEntriesService.sendLogToFollow(send);
    }
}
