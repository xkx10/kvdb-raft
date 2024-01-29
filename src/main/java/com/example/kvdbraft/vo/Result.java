package com.example.kvdbraft.vo;

import java.io.Serializable;

public class Result<T> implements Serializable {
    private boolean success;
    private String errorMessage;
    private T data;

    // Constructors
    public Result() {
    }

    public Result(boolean success, String errorMessage, T data) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    // Getter and Setter methods

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    // Factory methods for creating Result instances

    public static <T> Result<T> success(T data) {
        return new Result<>(true, null, data);
    }

    public static <T> Result<T> failure(String errorMessage) {
        return new Result<>(false, errorMessage, null);
    }
}
