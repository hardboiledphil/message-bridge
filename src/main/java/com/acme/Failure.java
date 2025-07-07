package com.acme;

public record Failure<T>(Throwable error) implements Result<T> {
    public boolean isFailure() {
        return true;
    }

    public boolean isSuccess() {
        return false;
    }

    public Throwable value() {
        return error;
    }
}