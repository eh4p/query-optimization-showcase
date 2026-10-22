# Executed Verification Record

Verified on **2026-10-09** on Linux using the checked-in seed, schema, and queries. This is a manual application verification record, not a performance benchmark or a test suite. The plans below are the actual strings returned by H2 `EXPLAIN ANALYZE`, with application log prefixes removed.

## Environment and build

| Check | Observed result |
|---|---|
| `java -version` | Oracle Java **25.0.4.1** |
| `./mvnw -version` | Apache Maven **3.9.16**, Java **25.0.4.1** |
| Spring Boot startup | **4.1.1** |
| Hibernate startup | **7.4.5.Final** |
| H2 startup | **2.4.240** |
| `./mvnw clean package` | **BUILD SUCCESS**, 15 Java sources, no tests to run |
| `java -jar target/query-optimization-showcase-1.0.0.jar` | Started successfully; all README curls, result comparisons and edge checks passed |
| `./mvnw spring-boot:run` | Started successfully, schema validated and fake data loaded |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=diagnostics` | Started successfully; all six analysis SELECTs executed and SQL logging/counts enabled |
| Seed | 50 accounts, 100 merchants, 50,000 transactions and 50,000 identical unindexed copies |
| Normal SQL logging | No Hibernate or JdbcTemplate debug SQL output |
| Final build/application logs | No warnings or errors from the final successful runs |

The execution environment initially blocked binding a local server socket inside its sandbox. Running the local API and curl checks with the environment's approved localhost access resolved that restriction. No application workaround or configuration change was needed.

## Endpoint checks

Every Linux README curl command was executed successfully against **both normal and diagnostics profiles**, and against the final packaged JAR. JSON values were compared, including merchant names, amounts, timestamps, and order.

| Check | Result |
|---|---|
| Missing-index violation / solution, `ACC-00001`, size 50 | Both HTTP 200; identical 50-item DTO arrays |
| Date violation / solution, `2026-08-15` | Both HTTP 200; identical 140-item DTO arrays |
| N+1 violation / solution, `ACC-00001`, size 50 | Both HTTP 200; identical 50-item DTO arrays |
| OFFSET page 2000 / equivalent deep cursor, size 20 | Both HTTP 200; identical complete page responses, ids 10000 through 9981 |
| First cursor page, size 20 | Ids 50000 through 49981; cursor `(2026-12-31T18:49:48, 49981)` |
| Next cursor page, size 20 | Ids 49980 through 49961; no duplicate/omitted boundary ids |
| Two cursor pages, size 7 | Ids 50000 through 49987 exactly once; page boundary splits a five-row shared-timestamp group |
| Terminal cursor `(2026-01-01T00:00:00, 6)` | Ids 5 through 1; `nextCursor=null` |
| Cursor after oldest row and OFFSET page 2500 | Empty items; `nextCursor=null` |
| Correctly formatted unknown account / empty date | HTTP 200 and empty arrays |
| Size 0 / size 101 | HTTP 400 |
| Negative page / malformed account / malformed date | HTTP 400 |
| Only one cursor field / nonpositive cursor id | HTTP 400 |

## Measured before/after behavior

| Scenario / operator | Violation | Solution |
|---|---:|---:|
| Account query: transaction `scanCount` | **50,001** | **50** |
| Daily query: transaction `scanCount` | **50,100** | **141** |
| Merchant history: Hibernate prepared statements for 50 DTOs | **51** | **1** |
| Deep pagination: inner transaction index `scanCount` | **40,021** | **26** |

These are **engine-specific scan counters and statement counts**, not latency measurements. H2 counters can include end-of-range probes and repeated accesses. They must not be interpreted as unique result rows, milliseconds, or PostgreSQL metrics. In the date violation H2 loops over merchants and scans transactions using a merchant foreign-key index, so its 50,100 counter is not a simple table-scan count. Pagination outer derived-table and merchant counters are separate from the transaction-index work.

The keyset script uses the cursor immediately before OFFSET 40,000. Both pagination scripts request 21 candidates for a 20-item page, keeping lookahead work equivalent. Neither executes a total-count query.

## Actual N+1 diagnostics

For the README's 50-item account workload, the following session-local counter lines were emitted:

```text
N+1 violation: 50 items, 51 prepared SQL statements in this Hibernate session
N+1 solution: 50 items, 1 prepared SQL statements in this Hibernate session
```

Representative executed Hibernate SQL (one lazy merchant SELECT is shown; **50** such statements were logged for the violation):

```sql
-- Violation: initial transaction query
select t1_0.id,t1_0.account_id,t1_0.amount,t1_0.created_at,t1_0.currency,t1_0.description,t1_0.merchant_id,t1_0.status from transactions t1_0 where t1_0.account_id=? order by t1_0.created_at desc,t1_0.id desc fetch first ? rows only;

-- Violation: repeated lazy merchant lookup
select m1_0.id,m1_0.category,m1_0.name from merchants m1_0 where m1_0.id=?;

-- Solution: one joined transaction/merchant query
select t1_0.id,t1_0.account_id,t1_0.amount,t1_0.created_at,t1_0.currency,t1_0.description,t1_0.merchant_id,m1_0.id,m1_0.category,m1_0.name,t1_0.status from transactions t1_0 join merchants m1_0 on m1_0.id=t1_0.merchant_id where t1_0.account_id=? order by t1_0.created_at desc,t1_0.id desc fetch first ? rows only;
```

## Full final H2 execution plans

These were returned by the scripts in `src/main/resources/sql/analysis/` inside the running application. Generated H2 constraint/index names are retained as observed.

### 01-missing-index-violation.sql

```sql
SELECT
    "T"."ID",
    "T"."ACCOUNT_ID",
    "M"."NAME" AS "MERCHANT_NAME",
    "T"."AMOUNT",
    "T"."CURRENCY",
    "T"."STATUS",
    "T"."CREATED_AT",
    "T"."DESCRIPTION"
FROM "PUBLIC"."TRANSACTIONS_UNINDEXED" "T"
    /* PUBLIC.TRANSACTIONS_UNINDEXED.tableScan */
    /* WHERE T.ACCOUNT_ID = 'ACC-00001'
    */
    /* scanCount: 50001 */
INNER JOIN "PUBLIC"."MERCHANTS" "M"
    /* PUBLIC.PRIMARY_KEY_6: ID = T.MERCHANT_ID */
    ON 1=1
    /* scanCount: 2000 */
WHERE ("T"."ACCOUNT_ID" = 'ACC-00001')
    AND ("M"."ID" = "T"."MERCHANT_ID")
ORDER BY 2, 7 DESC, 1 DESC
FETCH FIRST 50 ROWS ONLY
```

### 01-missing-index-solution.sql

```sql
SELECT
    "T"."ID",
    "T"."ACCOUNT_ID",
    "M"."NAME" AS "MERCHANT_NAME",
    "T"."AMOUNT",
    "T"."CURRENCY",
    "T"."STATUS",
    "T"."CREATED_AT",
    "T"."DESCRIPTION"
FROM "PUBLIC"."TRANSACTIONS" "T"
    /* PUBLIC.IDX_TRANSACTIONS_ACCOUNT_CREATED_AT: ACCOUNT_ID = 'ACC-00001' */
    /* WHERE T.ACCOUNT_ID = 'ACC-00001'
    */
    /* scanCount: 50 */
INNER JOIN "PUBLIC"."MERCHANTS" "M"
    /* PUBLIC.PRIMARY_KEY_6: ID = T.MERCHANT_ID */
    ON 1=1
    /* scanCount: 99 */
WHERE ("T"."ACCOUNT_ID" = 'ACC-00001')
    AND ("M"."ID" = "T"."MERCHANT_ID")
ORDER BY 2, 7 DESC, 1 DESC
FETCH FIRST 50 ROWS ONLY
/* index sorted */
```

### 02-non-sargable-violation.sql

```sql
SELECT
    "T"."ID",
    "T"."ACCOUNT_ID",
    "M"."NAME" AS "MERCHANT_NAME",
    "T"."AMOUNT",
    "T"."CURRENCY",
    "T"."STATUS",
    "T"."CREATED_AT",
    "T"."DESCRIPTION"
FROM "PUBLIC"."MERCHANTS" "M"
    /* PUBLIC.MERCHANTS.tableScan */
    /* scanCount: 101 */
INNER JOIN "PUBLIC"."TRANSACTIONS" "T"
    /* PUBLIC.CONSTRAINT_INDEX_F: MERCHANT_ID = M.ID */
    ON 1=1
    /* scanCount: 50100 */
WHERE (CAST("T"."CREATED_AT" AS DATE) = DATE '2026-08-15')
    AND ("M"."ID" = "T"."MERCHANT_ID")
ORDER BY 7 DESC, 1 DESC
```

### 02-non-sargable-solution.sql

```sql
SELECT
    "T"."ID",
    "T"."ACCOUNT_ID",
    "M"."NAME" AS "MERCHANT_NAME",
    "T"."AMOUNT",
    "T"."CURRENCY",
    "T"."STATUS",
    "T"."CREATED_AT",
    "T"."DESCRIPTION"
FROM "PUBLIC"."TRANSACTIONS" "T"
    /* PUBLIC.IDX_TRANSACTIONS_CREATED_AT_ID: CREATED_AT >= TIMESTAMP '2026-08-15 00:00:00'
        AND CREATED_AT < TIMESTAMP '2026-08-16 00:00:00'
     */
    /* WHERE (T.CREATED_AT >= TIMESTAMP '2026-08-15 00:00:00')
        AND (T.CREATED_AT < TIMESTAMP '2026-08-16 00:00:00')
    */
    /* scanCount: 141 */
INNER JOIN "PUBLIC"."MERCHANTS" "M"
    /* PUBLIC.PRIMARY_KEY_6: ID = T.MERCHANT_ID */
    ON 1=1
    /* scanCount: 280 */
WHERE ("M"."ID" = "T"."MERCHANT_ID")
    AND ("T"."CREATED_AT" >= TIMESTAMP '2026-08-15 00:00:00')
    AND ("T"."CREATED_AT" < TIMESTAMP '2026-08-16 00:00:00')
ORDER BY 7 DESC, 1 DESC
/* index sorted */
```

### 03-pagination-violation.sql

```sql
SELECT
    "T"."ID",
    "T"."ACCOUNT_ID",
    "M"."NAME" AS "MERCHANT_NAME",
    "T"."AMOUNT",
    "T"."CURRENCY",
    "T"."STATUS",
    "T"."CREATED_AT",
    "T"."DESCRIPTION"
FROM (
    SELECT
        "ID",
        "ACCOUNT_ID",
        "MERCHANT_ID",
        "AMOUNT",
        "CURRENCY",
        "STATUS",
        "CREATED_AT",
        "DESCRIPTION"
    FROM "PUBLIC"."TRANSACTIONS"
    ORDER BY 7 DESC, 1 DESC
    OFFSET 40000 ROWS
    FETCH NEXT 21 ROWS ONLY
) "T"
    /* SELECT
        ID,
        ACCOUNT_ID,
        MERCHANT_ID,
        AMOUNT,
        CURRENCY,
        STATUS,
        CREATED_AT,
        DESCRIPTION
    FROM PUBLIC.TRANSACTIONS
        /* PUBLIC.IDX_TRANSACTIONS_CREATED_AT_ID */
        /* scanCount: 40021 */
    ORDER BY 7 DESC, 1 DESC
    OFFSET 40000 ROWS
    FETCH NEXT 21 ROWS ONLY
    /* index sorted */
     */
    /* scanCount: 22 */
INNER JOIN "PUBLIC"."MERCHANTS" "M"
    /* PUBLIC.PRIMARY_KEY_6: ID = T.MERCHANT_ID */
    ON 1=1
    /* scanCount: 42 */
WHERE "M"."ID" = "T"."MERCHANT_ID"
ORDER BY 7 DESC, 1 DESC
```

### 03-pagination-solution.sql

```sql
SELECT
    "T"."ID",
    "T"."ACCOUNT_ID",
    "M"."NAME" AS "MERCHANT_NAME",
    "T"."AMOUNT",
    "T"."CURRENCY",
    "T"."STATUS",
    "T"."CREATED_AT",
    "T"."DESCRIPTION"
FROM (
    SELECT
        "ID",
        "ACCOUNT_ID",
        "MERCHANT_ID",
        "AMOUNT",
        "CURRENCY",
        "STATUS",
        "CREATED_AT",
        "DESCRIPTION"
    FROM "PUBLIC"."TRANSACTIONS"
    WHERE ("CREATED_AT" <= TIMESTAMP '2026-03-14 23:40:00')
        AND (("CREATED_AT" < TIMESTAMP '2026-03-14 23:40:00')
        OR (("CREATED_AT" = TIMESTAMP '2026-03-14 23:40:00')
        AND ("ID" < CAST(10001 AS BIGINT))))
    ORDER BY 7 DESC, 1 DESC
    FETCH FIRST 21 ROWS ONLY
) "T"
    /* SELECT
        ID,
        ACCOUNT_ID,
        MERCHANT_ID,
        AMOUNT,
        CURRENCY,
        STATUS,
        CREATED_AT,
        DESCRIPTION
    FROM PUBLIC.TRANSACTIONS
        /* PUBLIC.IDX_TRANSACTIONS_CREATED_AT_ID: CREATED_AT <= TIMESTAMP '2026-03-14 23:40:00' */
        /* scanCount: 26 */
    WHERE (CREATED_AT <= TIMESTAMP '2026-03-14 23:40:00')
        AND ((CREATED_AT < TIMESTAMP '2026-03-14 23:40:00')
        OR ((CREATED_AT = TIMESTAMP '2026-03-14 23:40:00')
        AND (ID < CAST(10001 AS BIGINT))))
    ORDER BY 7 DESC, 1 DESC
    FETCH FIRST 21 ROWS ONLY
    /* index sorted */
     */
    /* scanCount: 22 */
INNER JOIN "PUBLIC"."MERCHANTS" "M"
    /* PUBLIC.PRIMARY_KEY_6: ID = T.MERCHANT_ID */
    ON 1=1
    /* scanCount: 42 */
WHERE "M"."ID" = "T"."MERCHANT_ID"
ORDER BY 7 DESC, 1 DESC
```

## Scope and unverified areas

- Windows wrapper commands are documented equivalents; execution was verified on Linux only.
- PostgreSQL/MySQL were not started or measured. Their discussion in the README is conceptual production context.
- No latency, throughput, write-overhead, or production-concurrency benchmark was performed.
- No tests, frontend, Docker/deployment configuration, CI workflow, security configuration, or external-service dependencies were added.
- The repository is local and ready to publish; no remote GitHub repository was created or pushed as part of this verification.
