package com.example.queryoptimization.controller;

import com.example.queryoptimization.dto.TransactionPage;
import com.example.queryoptimization.dto.TransactionResponse;
import com.example.queryoptimization.service.QueryOptimizationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/query-optimization")
public class QueryOptimizationController {
    private final QueryOptimizationService service;

    public QueryOptimizationController(QueryOptimizationService service) {
        this.service = service;
    }

    @GetMapping("/missing-index/violation")
    public List<TransactionResponse> missingIndexViolation(
            @RequestParam @Pattern(regexp = "ACC-[0-9]{5}") String accountId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.missingIndexViolation(accountId, size);
    }

    @GetMapping("/missing-index/solution")
    public List<TransactionResponse> missingIndexSolution(
            @RequestParam @Pattern(regexp = "ACC-[0-9]{5}") String accountId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.missingIndexSolution(accountId, size);
    }

    @GetMapping("/non-sargable/violation")
    public List<TransactionResponse> nonSargableViolation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.nonSargableViolation(date);
    }

    @GetMapping("/non-sargable/solution")
    public List<TransactionResponse> nonSargableSolution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.nonSargableSolution(date);
    }

    @GetMapping("/n-plus-one/violation")
    public List<TransactionResponse> nPlusOneViolation(
            @RequestParam @Pattern(regexp = "ACC-[0-9]{5}") String accountId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.nPlusOneViolation(accountId, size);
    }

    @GetMapping("/n-plus-one/solution")
    public List<TransactionResponse> nPlusOneSolution(
            @RequestParam @Pattern(regexp = "ACC-[0-9]{5}") String accountId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.nPlusOneSolution(accountId, size);
    }

    @GetMapping("/pagination/violation")
    public TransactionPage offsetPagination(
            @RequestParam(defaultValue = "2000") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.offsetPagination(page, size);
    }

    @GetMapping("/pagination/solution")
    public TransactionPage keysetPagination(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) @Min(1) Long cursorId) {
        return service.keysetPagination(size, cursorCreatedAt, cursorId);
    }
}
