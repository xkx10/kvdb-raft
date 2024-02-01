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
    // 最后一个提交日志index
    private Integer lastLogIndex;
    // 最后一个提交日志任期
    private Long lastLogTerm;
}
