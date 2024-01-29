package com.example.kvdbraft.po.cache;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
@Component
@Data
public class LeaderVolatileState {
    private Map<String,Long> nextIndexMap;
    private Map<String,Long> matchIndex;
    @Autowired
    Cluster cluster;
    @Autowired
    VolatileState volatileState;
    @PostConstruct
    public void init() {
        // 在组件实例化后执行初始化逻辑
        nextIndexMap = new HashMap<>();
        matchIndex = new HashMap<>();
        for (String clusterId : cluster.getClusterIds()) {
            nextIndexMap.put(clusterId, volatileState.getCommitIndex() + 1);
            matchIndex.put(clusterId, volatileState.getCommitIndex());
        }
    }

}
