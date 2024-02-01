package com.example.kvdbraft.rpc.consumer;

import com.example.kvdbraft.dto.AppendEntriesDTO;
import com.example.kvdbraft.dto.AppendEntriesResponseDTO;
import com.example.kvdbraft.dto.RequestVoteDTO;
import com.example.kvdbraft.dto.RequestVoteResponseDTO;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.rpc.interfaces.ProviderService;
import com.example.kvdbraft.vo.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@Slf4j
public class ConsumerServiceImpl implements ConsumerService {
    private final Map<String, ProviderService> rpcMap = new ConcurrentHashMap<>();

    ExecutorService heartExecutor = Executors.newScheduledThreadPool(10);

    private static final String rpcVersion = "1.0";

    @Resource
    private VolatileState volatileState;

    @Resource
    private Cluster cluster;

    @Resource
    private PersistenceState persistenceState;


    // 主节点独立掌权的最小时间 本项目去选举超时时间的2/3
    public volatile long leaseReadTime;

    // 上次一心跳时间戳
    public volatile long preHeartBeatTime = 0;

    // 心跳间隔基数
    public final long heartBeatTick = 5 * 100;

    // TODO:上次商谈结果，是想用延迟队列实现超时效果

    @Override
    public Result<RequestVoteResponseDTO> sendElection(String url, RequestVoteDTO requestVoteDTO) {
        try {
            ProviderService providerService = getOrCreateReference(url);
            // 开启远程调用
            return providerService.handlerElection(requestVoteDTO);
        } catch (RpcException rpcException) {
            // RPC连接创建后发送请求出现异常（一般情况是对面机器宕机了）
            return Result.failure("RPC请求失败：" + rpcException.getMessage());
        } catch (Exception e) {
            // 其他异常
            return Result.failure("发生未知错误：" + e.getMessage());
        }
    }

    /**
     * 动态ip直连RPC 创建引用
     *
     * @param url
     * @param interfaceClass
     * @param <T>
     * @return
     */
    private <T> T createRpcReference(String url, Class<T> interfaceClass) {
        T rpcService = null;
        try {
            // ReferenceConfigCache会在内部进行连接配置缓存
            ReferenceConfig<T> referenceConfig = new ReferenceConfig<>();
            referenceConfig.setInterface(interfaceClass);
            referenceConfig.setUrl(url);
            referenceConfig.setVersion(rpcVersion);
            rpcService = referenceConfig.get();
        } catch (Exception e) {
            System.err.println("创建 RPC 引用时发生异常：" + e.getMessage());
        }
        return rpcService;
    }

    /**
     * 从缓存中获取对应的RPC Reference，或者创建Reference
     *
     * @param url
     * @return
     */
    private ProviderService getOrCreateReference(String url) {
        return rpcMap.computeIfAbsent(url, key -> createRpcReference(key, ProviderService.class));
    }

    @Override
    public List<Future<Boolean>> heartNotJudgeResult() {
        // 发送给所有节点心跳，所有节点处理心跳后返回
        if (volatileState.getStatus() != EStatus.Leader.status) {
            return null;
        }
        Set<String> follows = cluster.getNoMyselfClusterIds();
        List<Future<Boolean>> futureArrayList = new ArrayList<>();
        // 拿到其他节点的地址，除了自己的调用地址
        for (String peer : follows) {
            // 提交一个任务
            sendHeart(peer);
            // TODO:提交一个线程，heartExecutor
//            futureArrayList.add(RaftThreadPool.submit(() -> {
//                try {
//                    // 将心跳通过rpc发送给跟随者节点
//                    AentryResult aentryResult = getRpcClient().send(request);
//                    long term = aentryResult.getTerm();
//                    if (term > currentTerm) {
//                        log.error("self will become follower, he's term : {}, my term : {}", term, currentTerm);
//                        currentTerm = term;
//                        votedFor = null;
//                        status = NodeStatus.FOLLOWER;
//                        preElectionTime = System.currentTimeMillis();
//                        return false;
//                    }
//                    return true;
//                } catch (Exception e) {
//                    log.error("HeartBeatTask RPC Fail, request URL : {} ", request.getUrl());
//                    return false;
//                }
//            }));
        }
        return futureArrayList;
    }

    private boolean sendHeart(String url) {
        try {
            ProviderService providerService = getOrCreateReference(url);
            preHeartBeatTime = System.currentTimeMillis();
            // 心跳只关心 term 和 leaderID
            AppendEntriesDTO heartDTO = AppendEntriesDTO.builder()
                    .entries(null)
                    .leaderId(cluster.getId())
                    .term(persistenceState.getCurrentTerm())
                    // 记录自己的CommitIndex，如果是leader，则为leaderCommit
                    .leaderCommit(volatileState.getCommitIndex())
                    .build();
            AppendEntriesResponseDTO heartResult = providerService.handlerHeart(heartDTO);
            long term = heartResult.getTerm();
            if (term > persistenceState.getCurrentTerm()) {
                log.error("self will become follower, he's term : {}, my term : {}", term, persistenceState.getCurrentTerm());
                persistenceState.setCurrentTerm(term);
                persistenceState.setVotedFor(null);
                volatileState.setStatus(EStatus.Follower.status);
                // 刷新上次选举时间
                // preElectionTime = System.currentTimeMillis();
                return false;
            }
        } catch (Exception e) {
            log.error("HeartBeatTask RPC Fail, request URL : {} ", url);
            return false;
        }
        return true;
    }
}
