package com.gireesh.brokerage.dto;

import com.gireesh.brokerage.entity.TradingAccount;

import java.math.BigDecimal;

public record TradingAccountResponse(
        Long id,
        BigDecimal cash,
        BigDecimal investedCapital,
        BigDecimal realizedPnl,
        BigDecimal charges) {

    public static TradingAccountResponse from(TradingAccount account) {
        return new TradingAccountResponse(
                account.getId(),
                account.getCash(),
                account.getInvestedCapital(),
                account.getRealizedPnl(),
                account.getCharges());
    }
}
