package com.example.kvdbraft.po;

import com.example.kvdbraft.rpc.interfaces.ProviderService;
import lombok.Data;
import org.apache.dubbo.config.ReferenceConfig;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class Cluster {
    @Value("${cluster.nodes}")
    private List<String> clusterIds;


    private static volatile Cluster instance;


    private Cluster(){
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
