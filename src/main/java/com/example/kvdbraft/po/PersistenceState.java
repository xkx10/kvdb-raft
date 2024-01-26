package com.example.kvdbraft.po;

import lombok.Data;

import java.util.List;
@Data
public class PersistenceState {
    Integer currentTerm;
    String votedFor;
    List<Log> logs;
}
