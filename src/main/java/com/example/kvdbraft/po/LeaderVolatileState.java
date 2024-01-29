package com.example.kvdbraft.po;

import lombok.Data;
import lombok.Synchronized;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class LeaderVolatileState {
    private Map<String,Long> nextIndexMap;
    private Map<String,Long> matchIndex;
    private VolatileState volatileState = VolatileState.getInstance();
    private LeaderVolatileState(){
        nextIndexMap = new HashMap<>();
        matchIndex = new HashMap<>();
        for (String clusterId : Cluster.getInstance().getClusterIds()) {
            nextIndexMap.put(clusterId, volatileState.getCommitIndex() + 1);
            matchIndex.put(clusterId, volatileState.getCommitIndex());
        }
    }
    private static volatile LeaderVolatileState instance;
    public static LeaderVolatileState getInstance(){
        if(instance == null){
            synchronized (LeaderVolatileState.class){
                if(instance == null){
                    instance = new LeaderVolatileState();
                }
            }
        }
        return instance;
    }
}
