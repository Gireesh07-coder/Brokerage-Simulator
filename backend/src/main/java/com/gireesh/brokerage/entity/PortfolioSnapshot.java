package com.gireesh.brokerage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_snapshots", indexes = @Index(
        name = "idx_snapshot_account_time",
        columnList = "account_id,captured_at"))
public class PortfolioSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private TradingAccount account;

    @NotNull
    @Column(name = "portfolio_value", nullable = false, precision = 20, scale = 2)
    private BigDecimal value;

    @NotNull
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal cash;

    @NotNull
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal invested;

    @NotNull
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal realizedPnl;

    @NotNull
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal unrealizedPnl;

    @NotNull
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal drawdown;

    @Column(precision = 20, scale = 2)
    private BigDecimal peakValue;

    @Column(precision = 20, scale = 2)
    private BigDecimal drawdownAmount;

    @Column(precision = 12, scale = 6)
    private BigDecimal drawdownPercent;

    @NotNull
    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    public PortfolioSnapshot() {
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

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }

    public BigDecimal getInvested() {
        return invested;
    }

    public void setInvested(BigDecimal invested) {
        this.invested = invested;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public BigDecimal getDrawdown() {
        return drawdown;
    }

    public void setDrawdown(BigDecimal drawdown) {
        this.drawdown = drawdown;
    }

    public BigDecimal getPeakValue() {
        return peakValue;
    }

    public void setPeakValue(BigDecimal peakValue) {
        this.peakValue = peakValue;
    }

    public BigDecimal getDrawdownAmount() {
        return drawdownAmount;
    }

    public void setDrawdownAmount(BigDecimal drawdownAmount) {
        this.drawdownAmount = drawdownAmount;
    }

    public BigDecimal getDrawdownPercent() {
        return drawdownPercent;
    }

    public void setDrawdownPercent(BigDecimal drawdownPercent) {
        this.drawdownPercent = drawdownPercent;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }
}
