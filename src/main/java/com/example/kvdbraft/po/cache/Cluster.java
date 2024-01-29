package com.example.kvdbraft.po.cache;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 集群配置
 */
@Component
@Data
public class Cluster {

    @Value("${cluster.nodes}")
    private List<String> clusterIds;

}
