package com.example.kvdbraft.service;

import com.example.kvdbraft.dto.ClusterChangeDTO;
import com.example.kvdbraft.dto.ClusterChangeResponseDTO;

import java.util.Set;

public interface ClusterService {
    void startClusterChange(Set<String> newClusterIds);

    ClusterChangeResponseDTO handlerClusterChange(ClusterChangeDTO clusterChangeDTO);
}
