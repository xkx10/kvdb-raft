package com.example.kvdbraft.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequestVoteResponseDTO implements Serializable {
    private Long term;
    private boolean voteGranted;
}
