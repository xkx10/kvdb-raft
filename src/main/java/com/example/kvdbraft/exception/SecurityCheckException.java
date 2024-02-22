package com.example.kvdbraft.exception;

import lombok.Data;

@Data
public class SecurityCheckException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;


}
