package com.example.queryoptimization.dto;

import java.time.LocalDateTime;

public record TransactionCursor(LocalDateTime createdAt, Long id) {
}
