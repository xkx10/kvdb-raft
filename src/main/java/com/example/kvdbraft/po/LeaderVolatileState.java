package com.example.kvdbraft.po;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LeaderVolatileState {
    Map<String,Long> nextIndexMap;
    Map<String,Long> matchIndex;
}
