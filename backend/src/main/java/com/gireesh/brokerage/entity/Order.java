package com.gireesh.brokerage.entity;
import jakarta.persistence.*; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDateTime;
@Entity @Table(name="orders", indexes={@Index(name="idx_order_account",columnList="account_id"),@Index(name="idx_order_security",columnList="security_id"),@Index(name="idx_order_status",columnList="status")})
public class Order {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="account_id",nullable=false) private TradingAccount account;
 @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="security_id",nullable=false) private Security security;
 @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private OrderSide side;
 @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private OrderType type;
 @NotNull @Enumerated(EnumType.STRING) @Column(nullable=false,length=25) private OrderStatus status=OrderStatus.PENDING;
 @NotNull @Positive @Column(nullable=false) private Integer quantity;
 @PositiveOrZero @Digits(integer=18,fraction=6) @Column(precision=24,scale=6) private BigDecimal requestedPrice;
 @PositiveOrZero @Digits(integer=18,fraction=6) @Column(precision=24,scale=6) private BigDecimal executedPrice;
 @PositiveOrZero @Digits(integer=18,fraction=2) @Column(precision=20,scale=2) private BigDecimal charges=BigDecimal.ZERO;
 @Size(max=500) @Column(length=500) private String rejectionReason;
 @Column(nullable=false,updatable=false) private LocalDateTime createdAt; private LocalDateTime updatedAt; private LocalDateTime executedAt;
 @PrePersist void create(){if(status==null)status=OrderStatus.PENDING;if(charges==null)charges=BigDecimal.ZERO;createdAt=LocalDateTime.now();updatedAt=createdAt;} @PreUpdate void update(){updatedAt=LocalDateTime.now();}
 public Order(){} public Long getId(){return id;} public TradingAccount getAccount(){return account;} public void setAccount(TradingAccount v){account=v;} public Security getSecurity(){return security;} public void setSecurity(Security v){security=v;} public OrderSide getSide(){return side;} public void setSide(OrderSide v){side=v;} public OrderType getType(){return type;} public void setType(OrderType v){type=v;} public OrderStatus getStatus(){return status;} public void setStatus(OrderStatus v){status=v;} public Integer getQuantity(){return quantity;} public void setQuantity(Integer v){quantity=v;} public BigDecimal getRequestedPrice(){return requestedPrice;} public void setRequestedPrice(BigDecimal v){requestedPrice=v;} public BigDecimal getExecutedPrice(){return executedPrice;} public void setExecutedPrice(BigDecimal v){executedPrice=v;} public BigDecimal getCharges(){return charges;} public void setCharges(BigDecimal v){charges=v;} public String getRejectionReason(){return rejectionReason;} public void setRejectionReason(String v){rejectionReason=v;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;} public LocalDateTime getExecutedAt(){return executedAt;} public void setExecutedAt(LocalDateTime v){executedAt=v;}
}