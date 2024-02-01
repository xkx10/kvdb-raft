package com.example.kvdbraft.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Log {
    private Integer index;
    // 日志所在任期
    private Long term;
    private String command;
}
