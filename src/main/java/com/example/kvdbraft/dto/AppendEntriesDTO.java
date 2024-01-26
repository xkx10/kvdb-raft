package com.example.kvdbraft.dto;

import com.example.kvdbraft.po.Log;
import lombok.Data;

import java.util.List;
@Data
public class AppendEntriesDTO {
    private Long term;
    private String leaderId;
    private Long prevLogIndex;
    private Long prevLogTerm;
    List<Log> entries;
    private Long leaderCommit;
}
