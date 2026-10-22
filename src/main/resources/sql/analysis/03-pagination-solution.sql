EXPLAIN ANALYZE
SELECT t.id, t.account_id, m.name AS merchant_name, t.amount,
       t.currency, t.status, t.created_at, t.description
FROM (
    SELECT id, account_id, merchant_id, amount, currency, status, created_at, description
    FROM transactions
    WHERE created_at <= TIMESTAMP '2026-03-14 23:40:00'
      AND (created_at < TIMESTAMP '2026-03-14 23:40:00'
           OR (created_at = TIMESTAMP '2026-03-14 23:40:00' AND id < 10001))
    ORDER BY created_at DESC, id DESC
    LIMIT 21
) t JOIN merchants m ON m.id = t.merchant_id
ORDER BY t.created_at DESC, t.id DESC;
