package com.example.kvdbraft.dto;

import lombok.Data;

@Data
public class AppendEntriesResponseDTO {
    private Long term;
    private boolean success;
}
