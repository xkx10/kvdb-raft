package com.example.kvdbraft.service.impl;

import com.example.kvdbraft.dto.ClusterChangeDTO;
import com.example.kvdbraft.dto.ClusterChangeResponseDTO;
import com.example.kvdbraft.enums.EStatus;
import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.cache.Cluster;
import com.example.kvdbraft.po.cache.PersistenceState;
import com.example.kvdbraft.po.cache.VolatileState;
import com.example.kvdbraft.rpc.interfaces.ConsumerService;
import com.example.kvdbraft.service.AppendEntriesService;
import com.example.kvdbraft.service.ClusterService;
import com.example.kvdbraft.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class ClusterServiceImpl implements ClusterService {
    @Resource
    Cluster cluster;
    @Resource
    VolatileState volatileState;
    @Resource
    PersistenceState persistenceState;
    @Resource
    ConsumerService consumerService;
    @Resource
    AppendEntriesService appendEntriesService;

    ExecutorService clusterExecutor = Executors.newScheduledThreadPool(5);
    @Override
    public void startClusterChange(Set<String> newClusterIds) {
        if(cluster.isChangeStatus()){
            log.info("集群正在发生变更...");
            return;
        }
        if (volatileState.getStatus() != EStatus.Leader.status) {
            return;
        }
        cluster.setChangeStatus(true);
        cluster.setOldClusterIds(cluster.getClusterIds());
        cluster.setNewClusterIds(newClusterIds);

    }

    @Override
    public ClusterChangeResponseDTO handlerClusterChange(ClusterChangeDTO clusterChangeDTO) {
        return null;
    }

    /**
     * 一阶段提交
     */
    private boolean firstStageSubmit(){
        Log sendLog = Log.builder()
                .cluster(cluster)
                .build();
        return appendEntriesService.sendLogToFollow(sendLog);

    }
    private boolean sendCluster(String url, ClusterChangeDTO clusterChangeDTO){
        ClusterChangeResponseDTO result = consumerService.sendCluster(url, clusterChangeDTO).getData();
        if(result.getTerm() > persistenceState.getCurrentTerm()){
            return false;
        }
        return result.getSuccess();

    }

}
