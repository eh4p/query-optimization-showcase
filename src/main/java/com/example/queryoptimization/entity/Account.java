package com.example.queryoptimization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    private Long id;
    @Column(name = "account_number", nullable = false, length = 20, unique = true)
    private String accountNumber;
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    protected Account() {
    }
}
