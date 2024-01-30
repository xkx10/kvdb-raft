package com.example.kvdbraft.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
public class RequestVoteDTO implements Serializable{
    private Long term;
    private String candidateId;
    private Integer lastLogIndex;
    private Long lastLogTerm;
}
