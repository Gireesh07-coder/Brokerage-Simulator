package com.gireesh.brokerage.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_accounts", indexes = @Index(name = "idx_trading_account_user", columnList = "user_id"))
public class TradingAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @NotNull
    @Digits(integer = 18, fraction = 2)
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal cash;

    @NotNull
    @Digits(integer = 18, fraction = 2)
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal investedCapital = BigDecimal.ZERO;

    @NotNull
    @Digits(integer = 18, fraction = 2)
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @NotNull
    @Digits(integer = 18, fraction = 2)
    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal charges = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void create() {
        if (realizedPnl == null) {
            realizedPnl = BigDecimal.ZERO;
        }
        if (investedCapital == null) {
            investedCapital = BigDecimal.ZERO;
        }
        if (charges == null) {
            charges = BigDecimal.ZERO;
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void update() {
        updatedAt = LocalDateTime.now();
    }

    public TradingAccount() {
    }

    public TradingAccount(User user, BigDecimal cash) {
        this.user = user;
        this.cash = cash;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public BigDecimal getInvestedCapital() {
        return investedCapital;
    }

    public void setInvestedCapital(BigDecimal investedCapital) {
        this.investedCapital = investedCapital;
    }

    public BigDecimal getCharges() {
        return charges;
    }

    public void setCharges(BigDecimal charges) {
        this.charges = charges;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}