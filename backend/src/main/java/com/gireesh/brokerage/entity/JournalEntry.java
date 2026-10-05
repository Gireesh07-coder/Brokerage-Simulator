package com.gireesh.brokerage.entity;
import jakarta.persistence.*; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDateTime;
@Entity @Table(name="journal_entries", indexes=@Index(name="idx_journal_account",columnList="account_id"))
public class JournalEntry {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @NotNull @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="account_id",nullable=false) private TradingAccount account;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id") private Order order;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="trade_id") private Transaction trade;
 @Size(max=2000) private String thesis; @Size(max=2000) private String reason;
 @Enumerated(EnumType.STRING) private EmotionalState emotionalState;
 @DecimalMin("0") @DecimalMax("1") private BigDecimal confidence;
 @Enumerated(EnumType.STRING) private Strategy strategy; @Size(max=5000) private String notes; @Size(max=1000) private String outcome; @Size(max=2000) private String lessons;
 @Enumerated(EnumType.STRING) private MistakeCategory mistakeCategory;
 @Column(nullable=false,updatable=false) private LocalDateTime createdAt; private LocalDateTime updatedAt;
 @PrePersist void create(){createdAt=LocalDateTime.now();updatedAt=createdAt;} @PreUpdate void update(){updatedAt=LocalDateTime.now();}
 public JournalEntry(){} public Long getId(){return id;} public TradingAccount getAccount(){return account;} public void setAccount(TradingAccount v){account=v;} public Order getOrder(){return order;} public void setOrder(Order v){order=v;} public Transaction getTrade(){return trade;} public void setTrade(Transaction v){trade=v;} public String getThesis(){return thesis;} public void setThesis(String v){thesis=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;} public EmotionalState getEmotionalState(){return emotionalState;} public void setEmotionalState(EmotionalState v){emotionalState=v;} public BigDecimal getConfidence(){return confidence;} public void setConfidence(BigDecimal v){confidence=v;} public Strategy getStrategy(){return strategy;} public void setStrategy(Strategy v){strategy=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public String getOutcome(){return outcome;} public void setOutcome(String v){outcome=v;} public String getLessons(){return lessons;} public void setLessons(String v){lessons=v;} public MistakeCategory getMistakeCategory(){return mistakeCategory;} public void setMistakeCategory(MistakeCategory v){mistakeCategory=v;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
