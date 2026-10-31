package com.playground.api.exception;

public class SsrfSecurityException extends RuntimeException {
    public SsrfSecurityException(String message) {
        super(message);
    }
}
