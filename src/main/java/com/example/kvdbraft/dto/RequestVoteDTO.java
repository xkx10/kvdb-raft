package com.example.kvdbraft.dto;

import lombok.Data;

@Data
public class RequestVoteDTO {
    private Long term;
    private String candidateId;
    private Long lastLogIndex;
    private Long lastLogTerm;
}
