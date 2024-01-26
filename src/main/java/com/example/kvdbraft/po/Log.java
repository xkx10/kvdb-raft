package com.example.kvdbraft.po;

import lombok.Data;

@Data
public class Log {
    private Long index;
    private Long term;
    private String command;
}
