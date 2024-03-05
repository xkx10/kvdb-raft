package com.example.kvdbraft.po.cache;

import lombok.Data;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
@Component
@Data
public class LeaderVolatileState {
    private Map<String,Integer> nextIndexMap;
    private Map<String,Integer> matchIndexMap;
    @Resource
    Cluster cluster;
    @Resource
    VolatileState volatileState;

    public void initMap() {
        // 在组件实例化后执行初始化逻辑
        nextIndexMap = new HashMap<>();
        matchIndexMap = new HashMap<>();
        for (String clusterId : cluster.getClusterIds()) {
            nextIndexMap.put(clusterId, volatileState.getCommitIndex() + 1);
            matchIndexMap.put(clusterId, volatileState.getCommitIndex());
        }
    }

}
