ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(100);

ALTER TABLE orders
    ADD COLUMN request_fingerprint CHAR(64);

ALTER TABLE orders
    ADD CONSTRAINT uk_orders_account_idempotency_key
        UNIQUE (account_id, idempotency_key);
