package com.example.kvdbraft.service.impl.client.handler;

import com.example.kvdbraft.annotation.WriteOperation;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.Command;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.service.AppendEntriesService;
import com.example.kvdbraft.service.RocksService;
import com.example.kvdbraft.service.SecurityCheckService;
import com.example.kvdbraft.service.StateMachineService;
import com.example.kvdbraft.service.impl.client.OperationStrategy;
import lombok.extern.log4j.Log4j2;
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
    private ConsumerService consumerService;
    @Resource
    private SecurityCheckService securityCheckService;
    @Resource
    private VolatileState volatileState;
    @Resource
    private PersistenceState persistenceState;
    @Resource
    private AppendEntriesService appendEntriesService;
    @Resource
    private Cluster cluster;
    @Resource
    private StateMachineService stateMachineService;
    @Resource
    private RocksService rocksService;
    private int oneRpcTimeOut = 1000;
    private int sumRpcTimeOut = 1500;

    ExecutorService executor = Executors.newCachedThreadPool();

    ReentrantLock sendLogLock = new ReentrantLock();

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
        try {
            if (!sendLogLock.tryLock(1, TimeUnit.SECONDS)) {
                log.info("appendLock锁获取失败");
                return (T) Boolean.valueOf(false);
            }
            // 如果不是主节点，则进行rpc调用主节点的写日志请求
            if (volatileState.getStatus() != EStatus.Leader.status) {
                boolean rs = consumerService.writeLeader(volatileState.getLeaderId(), command).getData();
                return (T) Boolean.valueOf(rs);
            }
            // TODO:先假设command为set key value的形式
            String[] c = command.split(" ");
            Command cm = Command.builder()
                    .key(c[1])
                    .value(c[2])
                    .build();
            int lastIndex = volatileState.getLastIndex() + 1;
            Log writeLog = Log.builder()
                    .index(lastIndex)
                    .term(persistenceState.getCurrentTerm())
                    .command(cm).build();
            persistenceState.getLogs().add(writeLog);
            volatileState.setLastIndex(lastIndex);
            long start = System.currentTimeMillis();
            List<Future<Boolean>> futures = appendEntriesService.sendLogToFollow();
            AtomicInteger success = new AtomicInteger(1);
            CountDownLatch latch = new CountDownLatch(futures.size());
            for (Future<Boolean> future : futures) {
                executor.submit(() -> {
                    try {
                        Boolean res = future.get(oneRpcTimeOut, TimeUnit.MILLISECONDS);
                        if (res) {
                            success.incrementAndGet();
                        }
                    } catch (Exception e) {
                        log.error("从节点日志异步同步错误");
                    } finally {
                        //  每次调用这个方法，计数器的值就会减一。当计数器的值减到零时，所有因为调用 await() 方法而在等待的线程都会被唤醒
                        latch.countDown();
                    }
                });
            }
            // 等待三秒同步
            boolean await = latch.await(sumRpcTimeOut, TimeUnit.MILLISECONDS);
            if (!await) {
                log.error("存在一个或多个节点连接超时");
            }
            if (success.get() * 2 <= cluster.getClusterIds().size()) {
                // 回退日志和lastIndex
                persistenceState.getLogs().remove(lastIndex);
                volatileState.setLastIndex(lastIndex - 1);
                return (T) Boolean.valueOf(false);
            }
            long end = System.currentTimeMillis();
            // 写入成功了，更新commitIndex和应用到状态机，并且将状态进行持久化到rocks
            volatileState.setCommitIndex(lastIndex);
            log.info("日志写入成功 time = {} ", end - start);
            return (T) Boolean.valueOf(true);
        } catch (InterruptedException e) {
            log.error("线程异常中断 message = {}", e.getMessage(), e);
            return (T) Boolean.valueOf(false);
        } finally {
            sendLogLock.unlock();
        }
    }
}
