package com.example.kvdbraft.dto;

import lombok.Data;

@Data
public class RequestVoteResponseDTO {
    private Long term;
    private boolean voteGranted;
}
