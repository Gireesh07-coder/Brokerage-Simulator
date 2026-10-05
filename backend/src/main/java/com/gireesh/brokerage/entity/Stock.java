package com.gireesh.brokerage.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Retained for legacy schema rows and migration references; not an active domain model. */
@Entity
@Deprecated(since = "2026-10", forRemoval = false)
@Table(
        name = "stock",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_symbol",
                columnNames = "symbol"))
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String symbol;

    private String companyName;

    private BigDecimal currentPrice;

    public Stock() {
    }

    public Stock(String symbol, String companyName, BigDecimal currentPrice) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }
}