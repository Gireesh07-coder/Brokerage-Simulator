package com.gireesh.brokerage.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Account account;

    @ManyToOne
    private Stock stock;

    @ManyToOne
    @JoinColumn(name = "trading_account_id")
    private TradingAccount tradingAccount;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(precision = 20, scale = 2)
    private BigDecimal realizedPnl;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal tradeValue;

    private BigDecimal brokerage;

    private BigDecimal totalAmount;

    private LocalDateTime transactionTime;

    public Transaction() {
    }

    public Transaction(
            Account account,
            Stock stock,
            TransactionType type,
            Integer quantity,
            BigDecimal price,
            BigDecimal tradeValue,
            BigDecimal brokerage,
            BigDecimal totalAmount,
            LocalDateTime transactionTime) {

        this.account = account;
        this.stock = stock;
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.tradeValue = tradeValue;
        this.brokerage = brokerage;
        this.totalAmount = totalAmount;
        this.transactionTime = transactionTime;
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

    public TradingAccount getTradingAccount() {
        return tradingAccount;
    }

    public void setTradingAccount(TradingAccount tradingAccount) {
        this.tradingAccount = tradingAccount;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTradeValue() {
        return tradeValue;
    }

    public void setTradeValue(BigDecimal tradeValue) {
        this.tradeValue = tradeValue;
    }

    public BigDecimal getBrokerage() {
        return brokerage;
    }

    public void setBrokerage(BigDecimal brokerage) {
        this.brokerage = brokerage;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }
}