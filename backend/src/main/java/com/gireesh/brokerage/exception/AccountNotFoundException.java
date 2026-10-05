package com.gireesh.brokerage.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {

    public AccountNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                "Account not found with id: " + id);
    }
}