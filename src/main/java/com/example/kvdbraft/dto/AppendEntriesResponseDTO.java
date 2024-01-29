package com.example.kvdbraft.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppendEntriesResponseDTO implements Serializable {
    private Long term;
    private boolean success;
}
