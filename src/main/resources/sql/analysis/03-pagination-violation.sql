EXPLAIN ANALYZE
SELECT t.id, t.account_id, m.name AS merchant_name, t.amount,
       t.currency, t.status, t.created_at, t.description
FROM (
    SELECT id, account_id, merchant_id, amount, currency, status, created_at, description
    FROM transactions
    ORDER BY created_at DESC, id DESC
    LIMIT 21 OFFSET 40000
) t JOIN merchants m ON m.id = t.merchant_id
ORDER BY t.created_at DESC, t.id DESC;
