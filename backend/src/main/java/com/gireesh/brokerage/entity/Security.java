package com.gireesh.brokerage.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Phase 3 security projection; legacy Stock rows are linked by legacyStockId. */
@Entity
@Table(name = "securities", indexes = @Index(
        name = "idx_securities_symbol_unique",
        columnList = "symbol",
        unique = true))
public class Security {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private Long legacyStockId;
    @Column(nullable = false, unique = true)
    private String symbol;
    private String companyName;
    private BigDecimal currentPrice;
    private String exchange;
    private String sector;
    private BigDecimal previousClose;
    private Long volume;
    private boolean simulated = true;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    public Security() {}
    public Long getId() { return id; }
    public Long getLegacyStockId() { return legacyStockId; }
    public void setLegacyStockId(Long value) { legacyStockId = value; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String value) { symbol = value; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String value) { companyName = value; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal value) { currentPrice = value; }
    public String getExchange() { return exchange; }
    public void setExchange(String value) { exchange = value; }
    public String getSector() { return sector; }
    public void setSector(String value) { sector = value; }
    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal value) { previousClose = value; }
    public Long getVolume() { return volume; }
    public void setVolume(Long value) { volume = value; }
    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean value) { simulated = value; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime value) { createdAt = value; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime value) { updatedAt = value; }
}
