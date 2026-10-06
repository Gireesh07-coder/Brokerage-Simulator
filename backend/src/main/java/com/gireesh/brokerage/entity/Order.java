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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_account", columnList = "account_id"),
        @Index(name = "idx_order_security", columnList = "security_id"),
        @Index(name = "idx_order_status", columnList = "status")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_orders_account_idempotency_key",
        columnNames = {"account_id", "idempotency_key"}))
public class Order {
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private OrderStatus status = OrderStatus.PENDING;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer quantity;

    @PositiveOrZero
    @Digits(integer = 18, fraction = 6)
    @Column(precision = 24, scale = 6)
    private BigDecimal requestedPrice;

    @PositiveOrZero
    @Digits(integer = 18, fraction = 6)
    @Column(precision = 24, scale = 6)
    private BigDecimal executedPrice;

    @PositiveOrZero
    @Digits(integer = 18, fraction = 2)
    @Column(precision = 20, scale = 2)
    private BigDecimal charges = BigDecimal.ZERO;

    @Size(max = 500)
    @Column(length = 500)
    private String rejectionReason;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_fingerprint", length = 64)
    private String requestFingerprint;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private LocalDateTime executedAt;

    @PrePersist
    void create() {
        if (status == null) {
            status = OrderStatus.PENDING;
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

    public Order() {
    }

    public Long getId() { return id; }
    public TradingAccount getAccount() { return account; }
    public void setAccount(TradingAccount value) { account = value; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security value) { security = value; }
    public OrderSide getSide() { return side; }
    public void setSide(OrderSide value) { side = value; }
    public OrderType getType() { return type; }
    public void setType(OrderType value) { type = value; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus value) { status = value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { quantity = value; }
    public BigDecimal getRequestedPrice() { return requestedPrice; }
    public void setRequestedPrice(BigDecimal value) { requestedPrice = value; }
    public BigDecimal getExecutedPrice() { return executedPrice; }
    public void setExecutedPrice(BigDecimal value) { executedPrice = value; }
    public BigDecimal getCharges() { return charges; }
    public void setCharges(BigDecimal value) { charges = value; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String value) { rejectionReason = value; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String value) { idempotencyKey = value; }
    public String getRequestFingerprint() { return requestFingerprint; }
    public void setRequestFingerprint(String value) { requestFingerprint = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime value) { executedAt = value; }
}