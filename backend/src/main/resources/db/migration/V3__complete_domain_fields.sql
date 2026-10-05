ALTER TABLE trading_accounts
    ADD COLUMN IF NOT EXISTS invested_capital NUMERIC(20,2) NOT NULL DEFAULT 0;

ALTER TABLE securities ADD COLUMN IF NOT EXISTS exchange VARCHAR(32);
ALTER TABLE securities ADD COLUMN IF NOT EXISTS sector VARCHAR(100);
ALTER TABLE securities ADD COLUMN IF NOT EXISTS previous_close NUMERIC(20,6);
ALTER TABLE securities ADD COLUMN IF NOT EXISTS volume BIGINT;
ALTER TABLE securities ADD COLUMN IF NOT EXISTS simulated BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE securities ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE securities ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE securities
SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP),
    simulated = COALESCE(simulated, TRUE)
WHERE created_at IS NULL OR updated_at IS NULL;

ALTER TABLE positions ADD COLUMN IF NOT EXISTS realized_pnl NUMERIC(20,2) NOT NULL DEFAULT 0;
ALTER TABLE positions ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

UPDATE positions
SET created_at = COALESCE(created_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL;

ALTER TABLE trade_plans ADD COLUMN IF NOT EXISTS risk_amount NUMERIC(20,2);
ALTER TABLE trade_plans ADD COLUMN IF NOT EXISTS potential_profit NUMERIC(20,2);
ALTER TABLE trade_plans ADD COLUMN IF NOT EXISTS risk_reward_ratio NUMERIC(20,6);
ALTER TABLE trade_plans ADD COLUMN IF NOT EXISTS portfolio_risk_percent NUMERIC(12,6);
ALTER TABLE trade_plans ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE alerts ADD COLUMN IF NOT EXISTS security_id BIGINT;
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS position_id BIGINT;
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS threshold_value NUMERIC(20,6);
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS read_at TIMESTAMP;

ALTER TABLE alerts
    ADD CONSTRAINT fk_alert_security
        FOREIGN KEY (security_id) REFERENCES securities(id);

ALTER TABLE alerts
    ADD CONSTRAINT fk_alert_position
        FOREIGN KEY (position_id) REFERENCES positions(id);

ALTER TABLE portfolio_snapshots ADD COLUMN IF NOT EXISTS peak_value NUMERIC(20,2);
ALTER TABLE portfolio_snapshots ADD COLUMN IF NOT EXISTS drawdown_amount NUMERIC(20,2);
ALTER TABLE portfolio_snapshots ADD COLUMN IF NOT EXISTS drawdown_percent NUMERIC(12,6);

ALTER TABLE transactions
    ADD CONSTRAINT fk_transaction_trading_account
        FOREIGN KEY (trading_account_id) REFERENCES trading_accounts(id);

ALTER TABLE transactions
    ADD CONSTRAINT fk_transaction_order
        FOREIGN KEY (order_id) REFERENCES orders(id);

UPDATE trading_accounts ta
SET invested_capital = COALESCE((
    SELECT SUM(p.quantity * p.average_cost)
    FROM positions p
    WHERE p.account_id = ta.id
), 0);
