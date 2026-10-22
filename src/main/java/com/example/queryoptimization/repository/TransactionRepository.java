package com.example.queryoptimization.repository;

import com.example.queryoptimization.entity.Transaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    /** INTENTIONAL VIOLATION: mapping merchant names later triggers lazy loads. */
    @Query("""
            select t from Transaction t
            where t.accountId = :accountId
            order by t.createdAt desc, t.id desc
            """)
    List<Transaction> findTransactionsWithNPlusOne(@Param("accountId") String accountId, Pageable limit);

    // A to-one fetch join is compatible with this bounded query; a to-many join needs different paging.
    @Query("""
            select t from Transaction t
            join fetch t.merchant
            where t.accountId = :accountId
            order by t.createdAt desc, t.id desc
            """)
    List<Transaction> findTransactionsWithMerchantFetch(@Param("accountId") String accountId, Pageable limit);
}
