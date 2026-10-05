package com.gireesh.brokerage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_plans", indexes = {
        @Index(name = "idx_plan_account", columnList = "account_id"),
        @Index(name = "idx_plan_security", columnList = "security_id")
})
public class TradePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private TradingAccount account;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_id", nullable = false)
    private Security security;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderSide side;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 18, fraction = 6)
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal entryPrice;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 18, fraction = 6)
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal stopPrice;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 18, fraction = 6)
    @Column(nullable = false, precision = 24, scale = 6)
    private BigDecimal targetPrice;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    @Size(max = 2000)
    @Column(length = 2000)
    private String thesis;

    @Column(precision = 20, scale = 2)
    private BigDecimal riskAmount;

    @Column(precision = 20, scale = 2)
    private BigDecimal potentialProfit;

    @Column(precision = 20, scale = 6)
    private BigDecimal riskRewardRatio;

    @Column(precision = 12, scale = 6)
    private BigDecimal portfolioRiskPercent;

    @NotNull
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    public TradePlan() {
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

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(BigDecimal stopPrice) {
        this.stopPrice = stopPrice;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(BigDecimal targetPrice) {
        this.targetPrice = targetPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getThesis() {
        return thesis;
    }

    public void setThesis(String thesis) {
        this.thesis = thesis;
    }

    public BigDecimal getRiskAmount() {
        return riskAmount;
    }

    public void setRiskAmount(BigDecimal riskAmount) {
        this.riskAmount = riskAmount;
    }

    public BigDecimal getPotentialProfit() {
        return potentialProfit;
    }

    public void setPotentialProfit(BigDecimal potentialProfit) {
        this.potentialProfit = potentialProfit;
    }

    public BigDecimal getRiskRewardRatio() {
        return riskRewardRatio;
    }

    public void setRiskRewardRatio(BigDecimal riskRewardRatio) {
        this.riskRewardRatio = riskRewardRatio;
    }

    public BigDecimal getPortfolioRiskPercent() {
        return portfolioRiskPercent;
    }

    public void setPortfolioRiskPercent(BigDecimal portfolioRiskPercent) {
        this.portfolioRiskPercent = portfolioRiskPercent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
