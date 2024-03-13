package com.example.kvdbraft.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityCheckContext {
    Long rpcTerm;
    Integer rpcLastLogIndex;
    Long rpcLastLogTerm;
    int handlerType;
    String rpcNodeId;
}
