package com.example.queryoptimization.repository;

import com.example.queryoptimization.dto.TransactionResponse;
import com.example.queryoptimization.entity.Currency;
import com.example.queryoptimization.entity.TransactionStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TransactionQueries {
    private static final String COLUMNS = """
            SELECT t.id, t.account_id, m.name AS merchant_name, t.amount,
                   t.currency, t.status, t.created_at, t.description
            """;
    private static final String INDEXED_FROM = """
            FROM transactions t JOIN merchants m ON m.id = t.merchant_id
            """;
    private static final String ORDER = " ORDER BY t.created_at DESC, t.id DESC ";
    // account_id is constant under the equality filter; including it helps H2 match index order.
    private static final String ACCOUNT_ORDER = " ORDER BY t.account_id, t.created_at DESC, t.id DESC ";
    // Limit transaction rows before enriching them; both pagination versions share this shape.
    private static final String PAGE_FROM = """
            FROM (
                SELECT id, account_id, merchant_id, amount, currency, status, created_at, description
                FROM transactions
            """;
    private static final String PAGE_ORDER = " ORDER BY created_at DESC, id DESC ";
    private static final String PAGE_JOIN = ") t JOIN merchants m ON m.id = t.merchant_id" + ORDER;
    private static final RowMapper<TransactionResponse> ROW = (rs, row) -> new TransactionResponse(
            rs.getLong("id"), rs.getString("account_id"), rs.getString("merchant_name"),
            rs.getBigDecimal("amount"), Currency.valueOf(rs.getString("currency")),
            TransactionStatus.valueOf(rs.getString("status")),
            rs.getObject("created_at", LocalDateTime.class), rs.getString("description"));
    private final JdbcTemplate jdbc;

    public TransactionQueries(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** INTENTIONAL VIOLATION: no index has account_id as its leading column. */
    public List<TransactionResponse> findTransactionsWithoutAccountIndex(String accountId, int size) {
        return jdbc.query(COLUMNS + """
                FROM transactions_unindexed t JOIN merchants m ON m.id = t.merchant_id
                WHERE t.account_id = ?
                """ + ACCOUNT_ORDER + "LIMIT ?", ROW, accountId, size);
    }

    public List<TransactionResponse> findTransactionsWithAccountIndex(String accountId, int size) {
        return jdbc.query(COLUMNS + INDEXED_FROM + "WHERE t.account_id = ?" + ACCOUNT_ORDER + "LIMIT ?",
                ROW, accountId, size);
    }

    /** INTENTIONAL VIOLATION: transforms the indexed column before comparing it. */
    public List<TransactionResponse> findTransactionsWithNonSargablePredicate(LocalDate date) {
        return jdbc.query(COLUMNS + INDEXED_FROM + "WHERE CAST(t.created_at AS DATE) = ?" + ORDER,
                ROW, date);
    }

    public List<TransactionResponse> findTransactionsWithSargablePredicate(LocalDate date) {
        return jdbc.query(COLUMNS + INDEXED_FROM + "WHERE t.created_at >= ? AND t.created_at < ?" + ORDER,
                ROW, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    /** INTENTIONAL VIOLATION: work grows with the number of rows skipped. */
    public List<TransactionResponse> findTransactionsWithOffsetPagination(long offset, int limit) {
        return jdbc.query(COLUMNS + PAGE_FROM + PAGE_ORDER + "LIMIT ? OFFSET ? " + PAGE_JOIN,
                ROW, limit, offset);
    }

    public List<TransactionResponse> findFirstKeysetPage(int limit) {
        return jdbc.query(COLUMNS + PAGE_FROM + PAGE_ORDER + "LIMIT ? " + PAGE_JOIN, ROW, limit);
    }

    public List<TransactionResponse> findTransactionsWithKeysetPagination(
            LocalDateTime cursorCreatedAt, long cursorId, int limit) {
        // The redundant upper bound exposes a simple index range to H2's optimizer.
        // The second condition still handles duplicate timestamps correctly.
        return jdbc.query(COLUMNS + PAGE_FROM + """
                WHERE created_at <= ?
                  AND (created_at < ? OR (created_at = ? AND id < ?))
                """ + PAGE_ORDER + "LIMIT ? " + PAGE_JOIN, ROW,
                cursorCreatedAt, cursorCreatedAt, cursorCreatedAt, cursorId, limit);
    }
}
