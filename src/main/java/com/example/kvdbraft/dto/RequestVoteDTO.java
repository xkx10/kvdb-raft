package com.example.kvdbraft.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequestVoteDTO implements Serializable {
    private Long term;
    private String candidateId;
    private Long lastLogIndex;
    private Long lastLogTerm;
}
