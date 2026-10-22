package com.example.queryoptimization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "merchants")
public class Merchant {
    @Id
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 40)
    private String category;

    protected Merchant() {
    }

    public String getName() {
        return name;
    }
}
