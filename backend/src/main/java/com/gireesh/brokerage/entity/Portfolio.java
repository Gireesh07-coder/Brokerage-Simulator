package com.gireesh.brokerage.entity;

import jakarta.persistence.*;

/** Retained for legacy schema rows and migration references; not an active domain model. */
@Entity
@Deprecated(since = "2026-10", forRemoval = false)
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Account account;

    @ManyToOne
    private Stock stock;

    private Integer quantity;

    public Portfolio() {
    }

    public Portfolio(Account account, Stock stock, Integer quantity) {
        this.account = account;
        this.stock = stock;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}