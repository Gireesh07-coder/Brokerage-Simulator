package com.gireesh.brokerage.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "positions",
       uniqueConstraints = @UniqueConstraint(name = "uk_position_account_security",
                                              columnNames = {"account_id", "security_id"}))
public class Position {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private TradingAccount account;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private Security security;
    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal averageCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal realizedPnl = BigDecimal.ZERO;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }

    public Position() {}
    public Long getId() { return id; }
    public TradingAccount getAccount() { return account; }
    public void setAccount(TradingAccount value) { account = value; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security value) { security = value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { quantity = value; }
    public BigDecimal getAverageCost() { return averageCost; }
    public void setAverageCost(BigDecimal value) { averageCost = value; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal value) { realizedPnl = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
