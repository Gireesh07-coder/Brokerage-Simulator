package com.gireesh.brokerage.exception;

import org.springframework.http.HttpStatus;

public class TradingException extends ApiException {

    public TradingException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST, errorCode, message);
    }
}
