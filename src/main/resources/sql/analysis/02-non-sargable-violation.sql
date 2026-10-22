EXPLAIN ANALYZE
SELECT t.id, t.account_id, m.name AS merchant_name, t.amount,
       t.currency, t.status, t.created_at, t.description
FROM transactions t JOIN merchants m ON m.id = t.merchant_id
WHERE CAST(t.created_at AS DATE) = DATE '2026-08-15'
ORDER BY t.created_at DESC, t.id DESC;
