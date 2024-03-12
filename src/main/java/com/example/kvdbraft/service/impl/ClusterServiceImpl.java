package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.service.AppendEntriesService;
import com.example.kvdbraft.service.ClusterService;
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

    private final ReentrantLock clusterChangeLock = new ReentrantLock();


    @Override
    public Boolean startClusterChange(Set<String> newClusterIds) {
        try {
            if(!clusterChangeLock.tryLock(1, TimeUnit.SECONDS)){
                log.info("集群正在发生变更操作");
            }
            if(cluster.isChangeStatus()){
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
            if(!firstStageSubmitResult){
                // 一阶段提交失败可以直接放回false，对集群不会有影响
                log.info("一阶段提交失败 old = {}, new = {}", cluster.getOldClusterIds(), cluster.getNewClusterIds());
                return Boolean.valueOf(false);
            }
            // 二阶段提交
            cluster.setChangeStatus(false);
            cluster.setOldClusterIds(null);
            cluster.setNewClusterIds(null);
            cluster.setClusterIds(newClusterIds);
            boolean secondStageSubmitResult = stageSubmit();
            if(!secondStageSubmitResult){
                // 一阶段提交成功后，二阶段必须提交成功
                log.info("二阶段提交失败 old = {}, new = {}", cluster.getOldClusterIds(), cluster.getNewClusterIds());
                // 延迟一秒后重试
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    // 如果重试也失败应该上报监控平台，onCall相关人员进行操作，目前平台不具备这种能力所以只打印错误日志
                    log.error("重试被中断", e);
                    return Boolean.valueOf(false);
                }
                secondStageSubmitResult = stageSubmit();
                if (secondStageSubmitResult) {
                    log.info("重试成功");
                } else {
                    // 如果重试也失败应该上报监控平台，onCall相关人员进行操作，目前平台不具备这种能力所以只打印错误日志
                    log.error("重试失败");
                    return Boolean.valueOf(false);
                }
            }
            return Boolean.valueOf(true);
        } catch (InterruptedException e) {
            log.error("获取集群变更 clusterChangeLock 失败", e);
            return Boolean.valueOf(false);
        }finally {
            clusterChangeLock.unlock();
        }

    }


    /**
     * 集群变更提交
     */
    private boolean stageSubmit(){
        Log sendLog = Log.builder()
                .cluster(cluster)
                .build();
        return appendEntriesService.sendLogToFollow(sendLog);

    }


}
