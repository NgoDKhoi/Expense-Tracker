package com.example.expensetracker.core.domain;

public class Result<T> {
    private final Status status;
    private final T data;
    private final String message;
    private final Throwable exception;

    public enum Status {
        SUCCESS, ERROR, LOADING
    }

    private Result(Status status, T data, String message, Throwable exception) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.exception = exception;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(Status.SUCCESS, data, null, null);
    }

    public static <T> Result<T> error(String message, Throwable exception) {
        return new Result<>(Status.ERROR, null, message, exception);
    }

    public static <T> Result<T> loading() {
        return new Result<>(Status.LOADING, null, null, null);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getException() {
        return exception;
    }
}
