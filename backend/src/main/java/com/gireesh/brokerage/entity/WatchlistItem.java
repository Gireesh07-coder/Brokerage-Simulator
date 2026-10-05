package com.gireesh.brokerage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_watchlist_account_security",
        columnNames = {"account_id", "security_id"}))
public class WatchlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private TradingAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private Security security;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WatchlistItem() {
    }

    public WatchlistItem(TradingAccount account, Security security) {
        this.account = account;
        this.security = security;
    }

    @PrePersist
    void create() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(java.time.Clock.systemUTC());
        }
    }

    public Long getId() {
        return id;
    }

    public TradingAccount getAccount() {
        return account;
    }

    public void setAccount(TradingAccount account) {
        this.account = account;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
