package com.example.kvdbraft.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppendEntriesResponseDTO implements Serializable {
    private Long term;
    private boolean success;

    public AppendEntriesResponseDTO(boolean code){
        this.success = code;
    }

    public static AppendEntriesResponseDTO fail() {
        return new AppendEntriesResponseDTO(false);
    }

    public static AppendEntriesResponseDTO ok() {
        return new AppendEntriesResponseDTO(true);
    }
}
