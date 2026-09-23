package com.example.app.web;

import org.springframework.http.HttpStatus;

/** Erreur métier portant son code HTTP ; convertie en ProblemDetail par {@link ApiExceptionHandler}. */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
