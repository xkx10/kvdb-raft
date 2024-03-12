package com.example.kvdbraft.dto;

import com.example.kvdbraft.po.Log;
import com.example.kvdbraft.po.cache.Cluster;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterChangeDTO implements Serializable {
    private Long term;
    private String leaderId;
    private Cluster cluster;
}
