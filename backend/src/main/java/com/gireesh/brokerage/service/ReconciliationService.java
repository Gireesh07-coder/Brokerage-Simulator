package com.gireesh.brokerage.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Read-only migration smoke check. It deliberately compares legacy and
 * mapped row counts without modifying either side.
 */
@Service
public class ReconciliationService {
    private final JdbcTemplate jdbc;

    public ReconciliationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ReconciliationReport report() {
        long accounts = count("account");
        long mappedAccounts = count("trading_accounts");
        long stocks = count("stock");
        long mappedSecurities = count("securities");
        long legacyPositions = scalar("select count(*) from portfolio where quantity is not null");
        long mappedPositions = count("positions");
        long transactionsWithoutAccount = scalar(
                "select count(*) from transactions where account_id is not null and trading_account_id is null");
        return new ReconciliationReport(accounts, mappedAccounts, stocks, mappedSecurities,
                legacyPositions, mappedPositions, transactionsWithoutAccount);
    }

    private long count(String table) {
        return scalar("select count(*) from " + table);
    }

    private long scalar(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
