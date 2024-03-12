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
    private String id;

    /**
     * 变更状态，如果为true说明节点处于两阶段提交的中间状态
     * 这时候集群的决定需要old集群和new集群都有一半以上节点同意
     */
    private boolean changeStatus = false;

    private Set<String> oldClusterIds;
    private Set<String> newClusterIds;

    /**
     * 获取除自己节点以外的id
     * @return 节点列表
     */
    public Set<String> getNoMyselfClusterIds(){
        Set<String> noMyselfClusterIds = new HashSet<>(clusterIds);
        noMyselfClusterIds.remove(id);
        return noMyselfClusterIds;
    }
    public Set<String> getNoMyselfOldClusterIds(){
        Set<String> noMyselfClusterIds = new HashSet<>(oldClusterIds);
        noMyselfClusterIds.remove(id);
        return noMyselfClusterIds;
    }
    public Set<String> getNoMyselfNewClusterIds(){
        Set<String> noMyselfClusterIds = new HashSet<>(newClusterIds);
        noMyselfClusterIds.remove(id);
        return noMyselfClusterIds;
    }
}
