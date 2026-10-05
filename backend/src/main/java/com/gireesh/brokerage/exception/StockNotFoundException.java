package com.gireesh.brokerage.exception;

import org.springframework.http.HttpStatus;

public class StockNotFoundException extends ApiException {

    public StockNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "STOCK_NOT_FOUND",
                "Stock not found with id: " + id);
    }
}
