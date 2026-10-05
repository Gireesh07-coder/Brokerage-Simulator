package com.gireesh.brokerage.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ApiValidationException extends ApiException {
    private final Map<String, String> errors;

    public ApiValidationException(String error, String message, Map<String, String> errors) {
        super(HttpStatus.BAD_REQUEST, error, message);
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
