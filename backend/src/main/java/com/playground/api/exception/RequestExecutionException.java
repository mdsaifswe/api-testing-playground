package com.playground.api.exception;

import lombok.Getter;

@Getter
public class RequestExecutionException extends RuntimeException {
    private final String errorCategory;

    public RequestExecutionException(String message, String errorCategory) {
        super(message);
        this.errorCategory = errorCategory;
    }

    public RequestExecutionException(String message, Throwable cause, String errorCategory) {
        super(message, cause);
        this.errorCategory = errorCategory;
    }
}
