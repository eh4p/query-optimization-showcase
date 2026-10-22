package com.example.queryoptimization.dto;

import com.example.queryoptimization.entity.Currency;
import com.example.queryoptimization.entity.Transaction;
import com.example.queryoptimization.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String accountNumber,
        String merchantName,
        BigDecimal amount,
        Currency currency,
        TransactionStatus status,
        LocalDateTime createdAt,
        String description
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(), transaction.getAccountId(), transaction.getMerchant().getName(),
                transaction.getAmount(), transaction.getCurrency(), transaction.getStatus(),
                transaction.getCreatedAt(), transaction.getDescription());
    }
}
