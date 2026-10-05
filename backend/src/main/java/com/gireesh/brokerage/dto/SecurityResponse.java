package com.gireesh.brokerage.dto;

import com.gireesh.brokerage.entity.Security;

import java.math.BigDecimal;

public record SecurityResponse(
        Long id,
        String symbol,
        String companyName,
        BigDecimal currentPrice,
        String exchange,
        String sector,
        BigDecimal previousClose,
        Long volume,
        boolean simulated) {

    public static SecurityResponse from(Security security) {
        return new SecurityResponse(
                security.getId(),
                security.getSymbol(),
                security.getCompanyName(),
                security.getCurrentPrice(),
                security.getExchange(),
                security.getSector(),
                security.getPreviousClose(),
                security.getVolume(),
                security.isSimulated());
    }
}
