package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;

import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.Log;

import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.service.ElectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ElectionServiceImpl implements ElectionService {
    // 缓存线程池处理选举过程中的短期任务时可能是合适的
    ExecutorService electionExecutor = Executors.newCachedThreadPool();
    @Autowired
    private PersistenceState persistenceState;
    @Autowired
    private VolatileState volatileState;
    @Autowired
    private Cluster cluster;
    @Autowired
    ConsumerService consumerService;


    @Override
    public boolean startElection() {
        log.info("id = {} 发起超时选举投票，可能即将成为新的leader。cluster = {}", cluster.getId(), cluster.getClusterIds());
        checkSecurity();
        Map<String, Future<RequestVoteResponseDTO>> futureMap = new HashMap<>();
        //循环除自身外其他节点rpc地址
        for (String clusterId : cluster.getNoMyselfClusterIds()) {
            RequestVoteDTO requestVoteDTO = new RequestVoteDTO();
            requestVoteDTO.setCandidateId(cluster.getId());
            requestVoteDTO.setTerm(persistenceState.getCurrentTerm());

            if (volatileState.getCommitIndex() != -1) {
                Log lastLog = persistenceState.getLogs().get(volatileState.getCommitIndex());
                requestVoteDTO.setLastLogIndex(lastLog.getIndex());
                requestVoteDTO.setLastLogTerm(lastLog.getTerm());
            }

            Future<RequestVoteResponseDTO> future = electionExecutor.submit(() -> sendElection(clusterId, requestVoteDTO));
            // 记录每个节点发回来的回应
            futureMap.put(clusterId, future);
        }
        AtomicInteger success = new AtomicInteger(1);
        // 同步器
        CountDownLatch latch = new CountDownLatch(futureMap.size());
        for (String id : futureMap.keySet()) {
            Future<RequestVoteResponseDTO> future = futureMap.get(id);
            // 开多个线程去统计成功的数量
            electionExecutor.submit(() -> {
                try {
                    RequestVoteResponseDTO requestVoteResponseDTO = future.get(3, TimeUnit.SECONDS);
                    if (requestVoteResponseDTO.isVoteGranted()) {
                        success.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("error, message = 拉票RPC超时, id = {}", id, e);
                } finally {
                    //  每次调用这个方法，计数器的值就会减一。当计数器的值减到零时，所有因为调用 await() 方法而在等待的线程都会被唤醒
                    latch.countDown();
                }
            });
        }
        try {
            // 等待三秒同步
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (success.get() * 2 <= cluster.getClusterIds().size()) {
            log.info("vote fail, id = {} 未能成为leader, 获得投票数 = {}", cluster.getId(), success.get());
            return false;
        }
        doSuccessElection();
        log.info("vote success, id = {} 成为leader, 获得投票数 = {}", cluster.getId(), success.get());
        return true;
    }

    @Override
    public RequestVoteResponseDTO acceptElection(RequestVoteDTO requestVoteDTO) {
        return null;
    }

    private RequestVoteResponseDTO sendElection(String clusterId, RequestVoteDTO requestVoteDTO) {
        return (RequestVoteResponseDTO) consumerService.sendElection(clusterId, requestVoteDTO).getData();
    }

    private void doSuccessElection() {
        volatileState.setStatus(EStatus.Leader.status);
        // todo 发起空日志写入 用于同步follow节点的日志信息
    }

    private void checkSecurity() {
    }
}
