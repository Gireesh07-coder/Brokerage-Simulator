package com.gireesh.brokerage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;

/** Retained for legacy schema rows and migration references; not an active domain model. */
@Entity
@Deprecated(since = "2026-10", forRemoval = false)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private BigDecimal cashBalance;

    public Account() {
    }

    public Account(String name, BigDecimal cashBalance) {
        this.name = name;
        this.cashBalance = cashBalance;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }
}