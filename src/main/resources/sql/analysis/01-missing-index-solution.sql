EXPLAIN ANALYZE
SELECT t.id, t.account_id, m.name AS merchant_name, t.amount,
       t.currency, t.status, t.created_at, t.description
FROM transactions t JOIN merchants m ON m.id = t.merchant_id
WHERE t.account_id = 'ACC-00001'
ORDER BY t.account_id, t.created_at DESC, t.id DESC
LIMIT 50;
