package com.acme;

public sealed interface Result<T> permits Success, Failure {}
