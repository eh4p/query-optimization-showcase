CREATE TABLE accounts (
    id BIGINT PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    customer_name VARCHAR(100) NOT NULL
);
CREATE TABLE merchants (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(40) NOT NULL
);
CREATE TABLE transactions (
    id BIGINT PRIMARY KEY,
    account_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL REFERENCES merchants(id),
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    description VARCHAR(150) NOT NULL
);
CREATE INDEX idx_transactions_account_created_at
    ON transactions(account_id, created_at DESC, id DESC);
CREATE INDEX idx_transactions_created_at_id
    ON transactions(created_at DESC, id DESC);
-- account_id stays scalar in this demo: an H2 account FK would add a competing
-- account-only index and obscure the composite-index comparison.

-- Educational snapshot only: deliberately no secondary indexes or foreign keys.
-- H2 creates indexes for foreign keys, which would undermine the missing-index example.
CREATE TABLE transactions_unindexed (
    id BIGINT PRIMARY KEY,
    account_id VARCHAR(20) NOT NULL,
    merchant_id BIGINT NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    description VARCHAR(150) NOT NULL
);
