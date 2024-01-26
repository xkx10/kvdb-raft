package com.example.kvdbraft.po;

import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class Cluster {
    Set<String> clusterIds;
    private static volatile Cluster instance;
    private Cluster(){
        // todo 先写成静态的测试，思考：写到配置文件与集群变更之间的协调
        clusterIds = new HashSet<>();
        clusterIds.add("node1");
        clusterIds.add("node2");
        clusterIds.add("node3");
        clusterIds.add("node4");
        clusterIds.add("node5");
    };
    public static Cluster getInstance(){
        if(instance == null){
            synchronized (Cluster.class){
                if(instance == null){
                    instance = new Cluster();
                }
            }
        }
        return instance;
    }
}
