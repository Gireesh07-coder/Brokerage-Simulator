package com.gireesh.brokerage.service;

public record ReconciliationReport(
        long legacyAccounts,
        long mappedTradingAccounts,
        long legacyStocks,
        long mappedSecurities,
        long legacyPositions,
        long mappedPositions,
        long transactionsWithoutTradingAccount) {

    public boolean isConsistent() {
        return legacyAccounts == mappedTradingAccounts
                && legacyStocks == mappedSecurities
                && transactionsWithoutTradingAccount == 0;
    }
}
