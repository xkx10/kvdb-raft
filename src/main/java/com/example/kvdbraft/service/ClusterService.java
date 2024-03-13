package com.example.kvdbraft.service;

import java.util.Set;

public interface ClusterService {
    /**
     * 发起集群动态变更
     * @param newClusterIds
     * @return
     */
    Boolean startClusterChange(Set<String> newClusterIds);

    void clusterSelfCheckAndShutdown();

    void twoPhaseCommit();

}
