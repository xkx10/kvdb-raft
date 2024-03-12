package com.example.kvdbraft.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterChangeResponseDTO implements Serializable {
    private Long term;
    private Boolean success;
    public ClusterChangeResponseDTO(boolean code){
        this.success = code;
    }

    public static ClusterChangeResponseDTO fail() {
        return new ClusterChangeResponseDTO(false);
    }

    public static ClusterChangeResponseDTO ok() {
        return new ClusterChangeResponseDTO(true);
    }
}
