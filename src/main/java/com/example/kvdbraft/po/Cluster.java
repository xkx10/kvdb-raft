package com.example.kvdbraft.po;

import com.example.kvdbraft.rpc.interfaces.ProviderService;
import lombok.Data;
import org.apache.dubbo.config.ReferenceConfig;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class Cluster {
    Set<String> clusterIds;


    private static volatile Cluster instance;


    private Cluster(){

        clusterIds = new HashSet<>();
        clusterIds.add("dubbo://localhost:9011");
        clusterIds.add("dubbo://localhost:9012");
        clusterIds.add("dubbo://localhost:9013");
//        clusterIds.add("dubbo://localhost:9013");
//        clusterIds.add("dubbo://localhost:9014");
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
