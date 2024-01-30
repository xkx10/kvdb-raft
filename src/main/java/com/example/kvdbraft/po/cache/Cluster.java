package com.example.kvdbraft.po.cache;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 集群配置
 */
@Component
@Data
public class Cluster {

    @Value("${cluster.nodes}")
    private Set<String> clusterIds;

    @Value("${cluster.my-address}")
    private String Id;

    public Set<String> getNoMyselfClusterIds(){
        Set<String> noMyselfClusterIds = new HashSet<>(clusterIds);
        noMyselfClusterIds.remove(Id);
        return noMyselfClusterIds;
    }

}
