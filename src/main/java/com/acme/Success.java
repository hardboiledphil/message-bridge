package com.acme;

public record Success<T>(T value) implements Result<T> {

    public boolean isFailure() {
        return false;
    }

    public boolean isSuccess() {
        return true;
    }

    public Throwable error() {
        throw new IllegalStateException("Cannot get value from a Success");
    }
}

