package com.example.queryoptimization.service;

import com.example.queryoptimization.dto.TransactionCursor;
import com.example.queryoptimization.dto.TransactionPage;
import com.example.queryoptimization.dto.TransactionResponse;
import com.example.queryoptimization.repository.TransactionQueries;
import com.example.queryoptimization.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class QueryOptimizationService {
    private static final Logger log = LoggerFactory.getLogger(QueryOptimizationService.class);
    private final TransactionQueries queries;
    private final TransactionRepository repository;
    private final EntityManager entityManager;

    public QueryOptimizationService(TransactionQueries queries, TransactionRepository repository,
                                    EntityManager entityManager) {
        this.queries = queries;
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public List<TransactionResponse> missingIndexViolation(String accountId, int size) {
        return queries.findTransactionsWithoutAccountIndex(accountId, size);
    }

    public List<TransactionResponse> missingIndexSolution(String accountId, int size) {
        return queries.findTransactionsWithAccountIndex(accountId, size);
    }

    public List<TransactionResponse> nonSargableViolation(LocalDate date) {
        return queries.findTransactionsWithNonSargablePredicate(date);
    }

    public List<TransactionResponse> nonSargableSolution(LocalDate date) {
        if (date.equals(LocalDate.MAX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date must have a following day");
        }
        return queries.findTransactionsWithSargablePredicate(date);
    }

    public List<TransactionResponse> nPlusOneViolation(String accountId, int size) {
        return merchantHistory(accountId, size, false);
    }

    public List<TransactionResponse> nPlusOneSolution(String accountId, int size) {
        return merchantHistory(accountId, size, true);
    }

    private List<TransactionResponse> merchantHistory(String accountId, int size, boolean fetchMerchant) {
        Session session = entityManager.unwrap(Session.class);
        StatementCounter counter = null;
        if (session.getSessionFactory().getStatistics().isStatisticsEnabled()) {
            counter = new StatementCounter();
            session.addEventListeners(counter);
        }
        var limit = PageRequest.of(0, size);
        var transactions = fetchMerchant
                ? repository.findTransactionsWithMerchantFetch(accountId, limit)
                : repository.findTransactionsWithNPlusOne(accountId, limit);
        // Mapping stays inside the transaction, where merchant lazy loads are observable.
        var result = transactions.stream().map(TransactionResponse::from).toList();
        if (counter != null) {
            log.info("N+1 {}: {} items, {} prepared SQL statements in this Hibernate session",
                    fetchMerchant ? "solution" : "violation", result.size(), counter.count);
        }
        return result;
    }

    public TransactionPage offsetPagination(int page, int size) {
        return page(queries.findTransactionsWithOffsetPagination((long) page * size, size + 1), size);
    }

    public TransactionPage keysetPagination(int size, LocalDateTime cursorCreatedAt, Long cursorId) {
        if ((cursorCreatedAt == null) != (cursorId == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Supply both cursorCreatedAt and cursorId, or neither");
        }
        var rows = cursorCreatedAt == null
                ? queries.findFirstKeysetPage(size + 1)
                : queries.findTransactionsWithKeysetPagination(cursorCreatedAt, cursorId, size + 1);
        return page(rows, size);
    }

    private static TransactionPage page(List<TransactionResponse> rows, int size) {
        boolean hasMore = rows.size() > size;
        List<TransactionResponse> items = List.copyOf(rows.subList(0, Math.min(rows.size(), size)));
        TransactionCursor cursor = null;
        if (hasMore) {
            var last = items.getLast();
            cursor = new TransactionCursor(last.createdAt(), last.id());
        }
        return new TransactionPage(items, cursor);
    }

    // Request-local counts avoid the concurrency ambiguity of global statistics deltas.
    private static final class StatementCounter implements SessionEventListener {
        private static final long serialVersionUID = 1L;
        private int count;

        @Override
        public void jdbcPrepareStatementEnd() {
            count++;
        }
    }
}
