package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.service.AppendEntriesService;
import com.example.kvdbraft.service.ClusterService;
import com.example.kvdbraft.service.TriggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class ClusterServiceImpl implements ClusterService {
    @Resource
    Cluster cluster;
    @Resource
    VolatileState volatileState;

    @Resource
    AppendEntriesService appendEntriesService;
    @Resource
    TriggerService triggerService;


    private final ReentrantLock clusterChangeLock = new ReentrantLock();

    @Override
    public Boolean startClusterChange(Set<String> newClusterIds) {
        try {
            if (!clusterChangeLock.tryLock()) {
                log.info("集群正在发生变更操作");
            }
            if (cluster.isChangeStatus()) {
                log.info("集群正在发生变更...");
                return Boolean.valueOf(false);
            }
            if (volatileState.getStatus() != EStatus.Leader.status) {
                return Boolean.valueOf(false);
            }
            // 一阶段提交
            cluster.setChangeStatus(true);
            cluster.setOldClusterIds(cluster.getClusterIds());
            cluster.setNewClusterIds(newClusterIds);
            boolean firstStageSubmitResult = stageSubmit();
            if (!firstStageSubmitResult) {
                // 一阶段提交失败可以直接放回false，对集群不会有影响
                log.info("一阶段提交失败 old = {}, new = {}", cluster.getOldClusterIds(), cluster.getNewClusterIds());
                return Boolean.valueOf(false);
            }
            twoPhaseCommit();
            clusterSelfCheckAndShutdown();
            log.info("集群变更成功 old = {}， new={}", cluster.getOldClusterIds(), cluster.getNewClusterIds());
            return Boolean.valueOf(true);
        } catch (RuntimeException e){
            // 降级处理
            // 如果提交失败，这里一般是二阶段提交失败抛出异常，较大程度上说明该主节点对集群已经失去了掌控，可以主动下线，让新主来执行二阶段任务
            log.info("集群变更失败，节点主动卸任 message = {}", e.getMessage());
            // 主动卸任
            volatileState.setStatus(EStatus.Follower.status);
            // 开始超时选举任务，停止心跳任务
            triggerService.startElectionTask();
            triggerService.stopHeartTask();
            return Boolean.valueOf(false);

        }finally {
            clusterChangeLock.unlock();
        }

    }

    @Override
    public void clusterSelfCheckAndShutdown() {
        if(!cluster.isChangeStatus() && !cluster.getClusterIds().contains(cluster.getId())){
            // 节点已经集群变更成功，并且集群信息中没有自己，应该采取下线措施
            volatileState.setStatus(EStatus.Shutdown.status);
            triggerService.stopHeartTask();
            triggerService.stopElectionTask();
        }
    }

    @Override
    public void twoPhaseCommit() {
        try {
            if (!clusterChangeLock.tryLock()) {
                log.info("集群正在发生变更操作");
            }
            // 二阶段提交
            cluster.setChangeStatus(false);
            cluster.setOldClusterIds(null);
            cluster.setNewClusterIds(null);
            cluster.setClusterIds(cluster.getNewClusterIds());
            boolean secondStageSubmitResult = stageSubmit();
            if (!secondStageSubmitResult) {
                // 一阶段提交成功后，二阶段一定会提交成功，
                // 就算主节点宕机，新的主节点也会继续执行二阶段提交
                log.info("二阶段提交失败,一秒后即将重试 old = {}, new = {}", cluster.getOldClusterIds(), cluster.getNewClusterIds());
                // 延迟一秒后重试
                Thread.sleep(1000);
                secondStageSubmitResult = stageSubmit();
                if (secondStageSubmitResult) {
                    log.info("重试成功");
                } else {
                    // 如果重试也失败应该上报监控平台，onCall相关人员进行操作，目前平台不具备这种能力所以只打印错误日志
                    log.error("重试失败");
                    throw new RuntimeException("重试失败");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            clusterChangeLock.unlock();
        }
    }

    /**
     * 集群变更提交
     */
    private boolean stageSubmit() {
        Log sendLog = Log.builder()
                .cluster(cluster)
                .build();
        return appendEntriesService.sendLogToFollow(sendLog);

    }

}
