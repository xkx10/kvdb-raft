package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;

import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.Log;

import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.LeaderVolatileState;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.service.ElectionService;
import com.example.kvdbraft.service.HeartbeatService;
import com.example.kvdbraft.service.LogService;
import com.example.kvdbraft.service.SecurityCheckService;
import com.example.kvdbraft.service.TriggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class ElectionServiceImpl implements ElectionService {
    // 缓存线程池处理选举过程中的短期任务时可能是合适的
    ExecutorService electionExecutor = Executors.newCachedThreadPool();
    @Resource
    private PersistenceState persistenceState;
    @Resource
    private VolatileState volatileState;
    @Resource
    private Cluster cluster;
    @Resource
    ConsumerService consumerService;
    public final ReentrantLock voteLock = new ReentrantLock();

    @Resource
    TriggerService triggerService;
    @Resource
    SecurityCheckService securityCheckService;
    @Resource
    LogService logService;
    @Resource
    HeartbeatService heartbeatService;
    @Resource
    LeaderVolatileState leaderVolatileState;
    @Override
    public boolean startElection() {
        log.info("id = {} 发起超时选举投票，可能即将成为新的leader。cluster = {}", cluster.getId(), cluster.getClusterIds());
        // 设置身份为candidate
        volatileState.setStatus(EStatus.Candidate.status);
        // 自己任期 +1
        persistenceState.increaseTerm();
        // 开始超时选举拉票
        Map<String, Future<RequestVoteResponseDTO>> futureMap = new HashMap<>();
        //循环除自身外其他节点rpc地址
        RequestVoteDTO requestVoteDTO = new RequestVoteDTO();
        requestVoteDTO.setCandidateId(cluster.getId());
        requestVoteDTO.setTerm(persistenceState.getCurrentTerm());
        Log lastLog = persistenceState.getLogs().get(volatileState.getLastIndex());
        requestVoteDTO.setLastLogIndex(lastLog.getIndex());
        requestVoteDTO.setLastLogTerm(lastLog.getTerm());
        for (String clusterId : cluster.getNoMyselfClusterIds()) {
            Future<RequestVoteResponseDTO> future = electionExecutor.submit(() -> sendElection(clusterId, requestVoteDTO));
            // 记录每个节点发回来的回应
            futureMap.put(clusterId, future);
        }
        // 记录票数
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
            boolean await = latch.await(3, TimeUnit.SECONDS);
            if (!await){
                log.info("存在一个或多个节点连接超时");
            }
        } catch (InterruptedException e) {
            log.error("await选举rpc回应出现错误，message = {}", e.getMessage());
        }
        if (success.get() * 2 <= cluster.getClusterIds().size()) {
            log.info("vote fail, id = {} 未能成为leader, 获得投票数 = {}", cluster.getId(), success.get());
            return false;
        }
        // 这里做双重校验，防止选举成功但是集群已经出现新的leader做不必要的操作
        // 比如： 我现在拉到一半以上的投票了，rpc正在网络中，这个时候有另外一个节点因为网络分区导致一段时间一直在选举，他的任期变的很高，
        //       这个时候他的网络分区好了，他快速的拉到了新一轮的投票，不做判断这个时候会发生脑裂。
        // 下面判断只能较大程度避免脑裂情况，但是即便发生脑裂，任期低的这个leader在集群并没有写入日志的能力，因为大半节点任期要比他高，所以对集群不会有影响
        if(volatileState.getStatus() == EStatus.Follower.status){
            log.info("集群中已经有了新的leader id = {}, my id = {} 未能成为leader", volatileState.getLeaderId(), cluster.getId());
            return false;
        }
        doSuccessElection();
        log.info("election success, id = {} 成为leader, 获得投票数 = {}, term = {}", cluster.getId(), success.get(), persistenceState.getCurrentTerm());
        return true;
    }

    @Override
    public RequestVoteResponseDTO acceptElection(RequestVoteDTO requestVoteDTO) {
        // 安全性校验
        try {
            securityCheckService.voteSecurityCheck(requestVoteDTO);
        }catch (SecurityException e){
            log.error("安全校验不通过, message = {}", e.getMessage());
            return RequestVoteResponseDTO.builder()
                    .voteGranted(false)
                    .term(persistenceState.getCurrentTerm())
                    .build();
        }

        // 接收到投票请求就将自己的票投的节点
        volatileState.setStatus(EStatus.Follower.status);
        volatileState.setLeaderId(requestVoteDTO.getCandidateId());
        persistenceState.setCurrentTerm(requestVoteDTO.getTerm());
        persistenceState.setVotedFor(requestVoteDTO.getCandidateId());
        RequestVoteResponseDTO requestVoteResponseDTO = new RequestVoteResponseDTO();
        requestVoteResponseDTO.setVoteGranted(true);
        requestVoteResponseDTO.setTerm(requestVoteDTO.getTerm());
        triggerService.updateElectionTaskTime();
        log.info("vote success {} -> {}, term = {}", cluster.getId(), requestVoteDTO.getCandidateId(), persistenceState.getCurrentTerm());
        return requestVoteResponseDTO;
    }

    private RequestVoteResponseDTO sendElection(String clusterId, RequestVoteDTO requestVoteDTO) {
        return consumerService.sendElection(clusterId, requestVoteDTO).getData();
    }

    private void doSuccessElection() {
        volatileState.setStatus(EStatus.Leader.status);
        triggerService.stopElectionTask();
        triggerService.startHeartTask();

        heartbeatService.heartNotJudgeResult();
        leaderVolatileState.initMap();
        // todo 发起空日志写入 用于同步follow节点的日志信息


    }


}
