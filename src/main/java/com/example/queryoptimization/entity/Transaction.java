package com.example.queryoptimization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    private Long id;
    @Column(name = "account_id", nullable = false, length = 20)
    private String accountId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false, length = 150)
    private String description;

    protected Transaction() {
    }

    public Long getId() { return id; }
    public String getAccountId() { return accountId; }
    public Merchant getMerchant() { return merchant; }
    public BigDecimal getAmount() { return amount; }
    public Currency getCurrency() { return currency; }
    public TransactionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getDescription() { return description; }
}
