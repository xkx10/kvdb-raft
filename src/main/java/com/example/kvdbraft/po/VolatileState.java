package com.example.kvdbraft.po;

import lombok.Data;

@Data
public class VolatileState {
    private Long commitIndex;
    private Long lastApplied;
}
