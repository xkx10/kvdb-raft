package com.example.kvdbraft.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
public class RequestVoteResponseDTO implements Serializable{
    private Long term;
    private boolean voteGranted;
    private String id;
}
