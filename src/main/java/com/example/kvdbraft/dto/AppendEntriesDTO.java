package com.example.kvdbraft.dto;

import com.example.kvdbraft.po.Log;
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
public class AppendEntriesDTO implements Serializable {
    private Long term;
    private String leaderId;
    private Long prevLogIndex;
    private Long prevLogTerm;
    List<Log> entries;
    private Long leaderCommit;
}
