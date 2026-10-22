package com.example.queryoptimization.dto;

import java.util.List;

public record TransactionPage(List<TransactionResponse> items, TransactionCursor nextCursor) {
}
