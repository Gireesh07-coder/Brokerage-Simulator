# TradeGuard Phase 3A — Domain Model and Database Design

**Status:** Proposal only  
**Date:** 2026-09-23  
**Scope:** Domain model, relational schema, and migration design. No schema or application implementation is included in this phase.

## 1. Design goals and boundaries

TradeGuard is a deterministic paper-trading and risk-analytics platform. The model must preserve an auditable record of simulated money movement while supporting:

- paper orders and execution states;
- positions with average cost and realized/unrealized P&L;
- pre-trade plans and risk calculations;
- journal and review data;
- portfolio history for drawdown and performance analytics;
- non-predictive alerts.

This remains a modular monolith using Spring Boot, JPA, and PostgreSQL. The current Angular application is out of scope for Phase 3A.

The target model should not expose JPA entities directly from REST endpoints. DTOs and service APIs are implementation work for a later phase.

## 2. Current model and observed data

The current approved baseline contains:

| Entity/table | Current fields and behavior |
|---|---|
| `Account` / `account` | `id`, `name`, `cashBalance`. This is the current virtual cash account. |
| `Stock` / `stock` | `id`, `symbol`, `companyName`, `currentPrice`. `symbol` now has a database uniqueness constraint. |
| `Portfolio` / `portfolio` | `id`, `account_id`, `stock_id`, `quantity`. It represents an open holding but has no average cost or P&L fields. |
| `Transaction` / `transactions` | `id`, account, stock, BUY/SELL type, quantity, price, trade value, brokerage, total amount, transaction time. |
| `TransactionType` | `BUY`, `SELL`. |

The current local PostgreSQL database contains existing demo data, including account `#1`, stock rows, portfolio rows, and transaction rows. The design assumes that data is valuable and must not be deleted or reset.

The repository also currently has uncommitted worktree files from earlier development attempts. Those files are not treated as an implemented target schema in this proposal. The target migration must be based on the persisted baseline tables and must be reviewed before implementation.

## 3. Target model

### 3.1 User

`users`

| Field | Type | Rules |
|---|---|---|
| `id` | `BIGINT` | Primary key, identity |
| `email` | `VARCHAR(254)` | Required, normalized lowercase, unique |
| `password_hash` | `VARCHAR(255)` | Required; never store plaintext |
| `display_name` | `VARCHAR(120)` | Required |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | Required |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | Required |

One user owns one default paper-trading account for the MVP. The relationship should be one-to-one at the database level, but the account model should not prevent future multiple accounts if that becomes a product requirement.

### 3.2 TradingAccount

`trading_accounts`

| Field | Type | Rules |
|---|---|---|
| `id` | `BIGINT` | Primary key, identity |
| `user_id` | `BIGINT` | Required foreign key to `users`, unique for MVP |
| `cash_balance` | `NUMERIC(20,2)` | Required, non-negative invariant |
| `invested_capital` | `NUMERIC(20,2)` | Required; current cost basis of open positions |
| `realized_pnl` | `NUMERIC(20,2)` | Required, default zero |
| `total_charges` | `NUMERIC(20,2)` | Required, default zero |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | Required |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | Required |

`portfolio_market_value`, `unrealized_pnl`, and total account value should be calculated from current security prices and positions rather than stored as mutable account columns. Historical values belong in `portfolio_snapshots`.

The initial virtual capital for newly registered users is a configurable application value, currently proposed as `₹10,00,000.00`, not a hard-coded value in the database migration.

### 3.3 Security

The current `Stock` entity should evolve into the conceptual `Security` entity. A physical table rename is optional and should not be performed until all foreign keys and compatibility APIs are migrated.

Recommended target table: `securities`. A compatibility-first migration may retain `stock` temporarily and map the richer entity to that table.

| Field | Type | Rules |
|---|---|---|
| `id` | `BIGINT` | Primary key, preserve existing IDs where possible |
| `symbol` | `VARCHAR(32)` | Required, normalized uppercase, unique |
| `company_name` | `VARCHAR(200)` | Required |
| `exchange` | `VARCHAR(32)` | Required for seeded data, e.g. `NSE` |
| `sector` | `VARCHAR(100)` | Nullable when unavailable |
| `current_price` | `NUMERIC(20,6)` | Required for simulator |
| `previous_close` | `NUMERIC(20,6)` | Nullable during backfill |
| `volume` | `BIGINT` | Nullable or zero when not available |
| `simulated` | `BOOLEAN` | Required, true for MVP seeded data |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | Required |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | Required |

The current stock rows are preserved. New descriptive fields are backfilled with explicit defaults or nulls; they must not be fabricated as historical market facts.

### 3.4 Order

`orders`

An order is the user's instruction and its lifecycle. It is distinct from a transaction, which records an executed financial movement.

| Field | Type | Rules |
|---|---|---|
| `id` | `BIGINT` | Primary key |
| `account_id` | `BIGINT` | Required foreign key |
| `security_id` | `BIGINT` | Required foreign key |
| `side` | `VARCHAR(16)` | `BUY` or `SELL` |
| `order_type` | `VARCHAR(20)` | `MARKET`, `LIMIT`, `STOP_LOSS` |
| `quantity` | `INTEGER` | Required and positive |
| `requested_price` | `NUMERIC(20,6)` | Required for limit/stop orders; nullable for market |
| `executed_price` | `NUMERIC(20,6)` | Nullable until execution |
| `charges` | `NUMERIC(20,2)` | Default zero |
| `status` | `VARCHAR(16)` | Required lifecycle state |
| `rejection_reason` | `VARCHAR(500)` | Required when rejected |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | Required |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | Required |
| `executed_at` | `TIMESTAMP WITH TIME ZONE` | Nullable |
| `cancelled_at` | `TIMESTAMP WITH TIME ZONE` | Nullable |

Lifecycle:

```text
CREATED -> PENDING -> EXECUTED
                   -> REJECTED
                   -> CANCELLED
CREATED -> REJECTED
CREATED -> CANCELLED
```

For the MVP, market orders may transition through `CREATED` and `PENDING` within one transaction before becoming `EXECUTED`. Limit and stop-loss orders can remain `PENDING` until a deterministic simulator event makes them eligible.

An order should not be executed twice. A unique execution marker or an application-level idempotency key should be added when the order API is implemented.

### 3.5 Position

The existing `Portfolio` table should become the conceptual `Position` table. It is the current-state holding, not a history table.

Recommended target table: `positions`. During migration, existing `portfolio` rows should be copied into `positions` while retaining `portfolio` as a compatibility view/table until old APIs are retired.

| Field | Type | Rules |
|---|---|---|
| `id` | `BIGINT` | Primary key |
| `account_id` | `BIGINT` | Required foreign key |
| `security_id` | `BIGINT` | Required foreign key |
| `quantity` | `INTEGER` | Positive for persisted open positions |
| `average_entry_price` | `NUMERIC(20,6)` | Required for new positions |
| `invested_value` | `NUMERIC(20,2)` | Current cost basis |
| `realized_pnl` | `NUMERIC(20,2)` | Optional denormalized contribution; transaction ledger remains authoritative |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | Required |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | Required |

There must be one position per `(account_id, security_id)`. A complete sale removes the row or closes it according to the chosen implementation; the MVP should remove zero-quantity rows and preserve the sale in the transaction ledger.

Unrealized P&L is calculated dynamically:

```text
(current_price - average_entry_price) × quantity
```

### 3.6 Transaction

The existing `transactions` table should remain the immutable execution/cash ledger and be extended rather than replaced.

Existing fields to retain:

- `id`
- account reference
- stock/security reference
- `type` (`BUY`, `SELL`)
- `quantity`
- `price`
- `tradeValue`
- `brokerage`
- `totalAmount`
- `transactionTime`

Recommended additions:

| Field | Type | Purpose |
|---|---|---|
| `order_id` | `BIGINT` | Links execution to the originating order, nullable for migrated legacy rows |
| `gross_amount` | `NUMERIC(20,2)` | Explicit trade value before charges |
| `brokerage` | `NUMERIC(20,2)` | Existing component |
| `stt` | `NUMERIC(20,2)` | Educational approximation |
| `exchange_charges` | `NUMERIC(20,2)` | Educational approximation |
| `gst` | `NUMERIC(20,2)` | Educational approximation |
| `sebi_charges` | `NUMERIC(20,2)` | Educational approximation |
| `stamp_duty` | `NUMERIC(20,2)` | Educational approximation |
| `total_charges` | `NUMERIC(20,2)` | Sum of all components |
| `net_amount` | `NUMERIC(20,2)` | Cash impact |
| `executed_at` | `TIMESTAMP WITH TIME ZONE` | Immutable execution timestamp |

Existing `tradeValue` can be retained for compatibility and mapped to `gross_amount` later. Legacy `brokerage` values must not be reinterpreted as a full current cost breakdown.

### 3.7 TradePlan

`trade_plans`

| Field | Type | Purpose |
|---|---|---|
| `id` | `BIGINT` | Primary key |
| `account_id` | `BIGINT` | Owner |
| `security_id` | `BIGINT` | Planned security |
| `side` | `VARCHAR(16)` | BUY or SELL |
| `entry_price` | `NUMERIC(20,6)` | Planned entry |
| `quantity` | `INTEGER` | Planned quantity |
| `stop_loss` | `NUMERIC(20,6)` | Planned invalidation level |
| `target` | `NUMERIC(20,6)` | Planned target |
| `risk_amount` | `NUMERIC(20,2)` | Calculated maximum loss |
| `potential_profit` | `NUMERIC(20,2)` | Calculated gross potential |
| `risk_reward_ratio` | `NUMERIC(20,6)` | Calculated ratio |
| `portfolio_risk_percent` | `NUMERIC(12,6)` | Risk amount / portfolio value |
| `status` | `VARCHAR(20)` | `DRAFT`, `REVIEWED`, `EXECUTED`, `CANCELLED`, `EXPIRED` |
| `created_at` / `updated_at` | timestamp | Lifecycle auditing |

The calculations should be produced by a deterministic service and stored as a snapshot of what the user reviewed. They are not investment recommendations.

### 3.8 RiskAssessment

Risk assessments should be **calculated dynamically by default**, not persisted as a separate table for every preview.

Reasons:

- the result depends on current cash, portfolio value, current price, and configured charges;
- persisting every draft preview creates noise and stale records;
- the trade plan can store the reviewed calculation snapshot when the user saves a plan;
- the order and transaction records provide the audit trail for actions actually taken.

A persisted `risk_assessments` table is justified only if product requirements later need immutable compliance/audit snapshots for every submitted order. If added later, it should reference `trade_plan_id` and/or `order_id`, include the formula inputs, calculation version, and calculated outputs.

### 3.9 JournalEntry

`journal_entries`

| Field | Type | Rules |
|---|---|---|
| `id` | `BIGINT` | Primary key |
| `account_id` | `BIGINT` | Required owner |
| `trade_id` or `order_id` | `BIGINT` | Nullable reference to the related execution |
| `thesis` | `TEXT` | User's thesis |
| `entry_reason` | `TEXT` | Why the trade was entered |
| `strategy` | `VARCHAR(50)` | Controlled enum or display value |
| `emotional_state` | `VARCHAR(50)` | Controlled enum |
| `confidence` | `NUMERIC(5,2)` | Recommended range 0–100 |
| `notes` | `TEXT` | Free-form notes |
| `outcome` | `TEXT` | Post-trade outcome |
| `lessons` | `TEXT` | What the user learned |
| `mistake_category` | `VARCHAR(50)` | Nullable controlled category |
| `created_at` / `updated_at` | timestamp | Auditing |

Journal text is user-authored. It must not be used to generate investment recommendations.

### 3.10 Alert

`alerts`

| Field | Type | Purpose |
|---|---|---|
| `id` | `BIGINT` | Primary key |
| `account_id` | `BIGINT` | Required owner |
| `type` | `VARCHAR(40)` | `TARGET_REACHED`, `STOP_REACHED`, `CONCENTRATION_THRESHOLD`, `DRAWDOWN_THRESHOLD` |
| `security_id` | `BIGINT` | Nullable; relevant for target/stop |
| `position_id` | `BIGINT` | Nullable |
| `message` | `VARCHAR(1000)` | Human-readable neutral message |
| `threshold_value` | `NUMERIC(20,6)` | Value that triggered the alert |
| `read_at` | timestamp | Nullable read marker |
| `created_at` | timestamp | Required |

Alerts report observed conditions. They do not predict prices or instruct the user to buy or sell.

### 3.11 PortfolioSnapshot

`portfolio_snapshots`

Snapshots provide the time series needed for equity curves, drawdown, and performance charts.

| Field | Type | Purpose |
|---|---|---|
| `id` | `BIGINT` | Primary key |
| `account_id` | `BIGINT` | Required owner |
| `captured_at` | `TIMESTAMP WITH TIME ZONE` | Required |
| `cash_balance` | `NUMERIC(20,2)` | Cash at capture |
| `invested_value` | `NUMERIC(20,2)` | Market value of open positions |
| `portfolio_value` | `NUMERIC(20,2)` | Cash plus market value |
| `realized_pnl` | `NUMERIC(20,2)` | Cumulative realized P&L |
| `unrealized_pnl` | `NUMERIC(20,2)` | P&L at capture |
| `peak_value` | `NUMERIC(20,2)` | Peak observed value through capture |
| `drawdown_amount` | `NUMERIC(20,2)` | Peak minus current value |
| `drawdown_percent` | `NUMERIC(12,6)` | Drawdown percentage |

Recommended uniqueness: `(account_id, captured_at)`. Capture frequency should be defined by the application, such as one snapshot per simulated market event or once per day, rather than on every frontend refresh.

## 4. Relationships

```text
User 1 ─── 1 TradingAccount

TradingAccount 1 ─── N Order
TradingAccount 1 ─── N Position
TradingAccount 1 ─── N Transaction
TradingAccount 1 ─── N TradePlan
TradingAccount 1 ─── N JournalEntry
TradingAccount 1 ─── N Alert
TradingAccount 1 ─── N PortfolioSnapshot

Security 1 ─── N Order
Security 1 ─── N Position
Security 1 ─── N Transaction
Security 1 ─── N TradePlan
Security 1 ─── N Alert

Order 1 ─── 0..N Transaction
Order 1 ─── 0..N JournalEntry
TradePlan 1 ─── 0..1 Order
Position 1 ─── 0..N Alert
```

The order-to-transaction relationship is intentionally separate: rejected and cancelled orders have no transaction, while an executed order creates one or more immutable transaction records if the design later supports fills.

## 5. Migration strategy

### 5.1 Principles

1. Back up the PostgreSQL database before any migration.
2. Never truncate or reset `account`, `stock`, `portfolio`, or `transactions`.
3. Preserve existing primary keys where practical.
4. Add new nullable columns first; backfill; then add non-null constraints only after verification.
5. Keep compatibility mappings/endpoints until the new services are proven.
6. Make each migration idempotent and reviewable.

### 5.2 Existing Account #1

Create one migration-time user representing the existing demo account only if a product-approved demo identity is defined. Do not invent a real password or commit credentials.

Then:

1. Insert a `users` row with a neutral migration email or an explicitly configured demo email.
2. Insert one `trading_accounts` row linked to that user.
3. Copy `account.id` to a legacy-reference column or maintain an explicit mapping table:

   ```text
   account_migration_map(legacy_account_id, trading_account_id)
   ```

4. Initialize `cash_balance` from `account.cashBalance`.
5. Initialize `realized_pnl` to zero unless historical realized P&L can be reconstructed from a verified cost-basis algorithm.
6. Initialize `invested_capital` from migrated positions after average cost is calculated.
7. Preserve the original `account` row during the compatibility period.

The mapping table is safer than assuming old and new generated IDs will match.

### 5.3 Existing Stock rows

Option A, recommended for the first migration: retain the physical `stock` table and evolve it in place into the Security model by adding `exchange`, `sector`, `previous_close`, `volume`, `simulated`, and timestamps.

Option B, later: create `securities`, copy rows preserving IDs, update all foreign keys, verify row counts and references, then retain `stock` as a compatibility view until old APIs are removed.

For current rows:

- preserve `id`, `symbol`, `companyName`, and `currentPrice`;
- set `simulated = true`;
- set exchange/sector only from known seed metadata;
- leave unavailable previous-close/volume values null or explicitly zero according to application rules;
- verify symbol uniqueness before adding the constraint.

### 5.4 Existing Portfolio rows

For every existing `portfolio` row:

1. Resolve the legacy account to its new trading account.
2. Resolve the legacy stock to its security.
3. Copy `quantity`.
4. Calculate `average_entry_price` from historical BUY transactions for that account/security:

   ```text
   average_entry_price =
       total historical BUY trade value / total historical BUY quantity
   ```

   If the historical data is incomplete or contains prior sells, use a FIFO/weighted-average reconstruction and record the method in the migration report.
5. Set `invested_value = quantity × average_entry_price`.
6. Create a unique `(account_id, security_id)` position.
7. Do not retain zero-quantity rows as open positions.

If a reliable cost basis cannot be reconstructed, preserve the position and mark the migration exception for review rather than silently fabricating a value.

### 5.5 Existing Transaction rows

Keep every legacy transaction. Add the new account/security foreign-key representation through a mapping or compatibility columns.

For each row:

- preserve type, quantity, price, trade value, brokerage, total amount, and original timestamp;
- link the row to the migrated trading account and security;
- leave `order_id` null because no historical order record exists;
- map existing brokerage into the brokerage component only;
- do not pretend that historical brokerage equals the complete new cost model;
- set `executed_at` from `transactionTime`.

Run reconciliation checks:

```text
legacy transaction count = migrated transaction count
legacy BUY quantity/value totals = migrated BUY totals
legacy SELL quantity/value totals = migrated SELL totals
legacy account cash = reconstructed cash within documented rounding tolerance
legacy portfolio quantities = migrated open position quantities
```

## 6. Constraints and indexes

### Primary and foreign keys

- Identity primary key on every table.
- Foreign keys from every account-owned record to `trading_accounts`.
- Foreign keys from orders, positions, transactions, and plans to securities.
- Explicit delete behavior: restrict deletion of securities/accounts with financial history; archive instead.

### Unique constraints

- `users.email` unique, normalized lowercase.
- `securities.symbol` unique, normalized uppercase.
- `positions(account_id, security_id)` unique.
- `trading_accounts.user_id` unique for the one-account MVP.
- `portfolio_snapshots(account_id, captured_at)` unique.
- Optional client idempotency key unique per account for order submission.

### Recommended indexes

- `users(email)`.
- `trading_accounts(user_id)`.
- `orders(account_id, created_at)`.
- `orders(account_id, status)`.
- `orders(security_id, status)`.
- `positions(account_id)`.
- `transactions(account_id, executed_at)`.
- `transactions(security_id, executed_at)`.
- `trade_plans(account_id, created_at)`.
- `journal_entries(account_id, created_at)`.
- `alerts(account_id, read_at, created_at)`.
- `portfolio_snapshots(account_id, captured_at)`.

## 7. Money, quantity, and rounding

- Use `BigDecimal` in Java and PostgreSQL `NUMERIC` for all monetary values.
- Use `NUMERIC(20,2)` for rupee cash, charges, P&L, and totals.
- Use `NUMERIC(20,6)` for prices and calculated ratios where sub-paisa precision is useful before final cash rounding.
- Use integer quantity for whole-share MVP trading.
- Round cash movements and charge components to two decimal places using an explicit `RoundingMode`, preferably `HALF_UP` for this educational simulator.
- Persist the rounded amount used in the ledger; never recompute historical totals from current configuration.
- Store the transaction-cost configuration version or rate snapshot with each executed transaction once the configurable cost engine is introduced.

## 8. Enums

Recommended persisted string enums:

- `OrderSide`: `BUY`, `SELL`
- `OrderType`: `MARKET`, `LIMIT`, `STOP_LOSS`
- `OrderStatus`: `CREATED`, `PENDING`, `EXECUTED`, `CANCELLED`, `REJECTED`
- `TradePlanStatus`: `DRAFT`, `REVIEWED`, `EXECUTED`, `CANCELLED`, `EXPIRED`
- `TransactionType`: retain `BUY`, `SELL`; add only ledger-specific types if cash funding/withdrawal is later recorded
- `AlertType`: `TARGET_REACHED`, `STOP_REACHED`, `CONCENTRATION_THRESHOLD`, `DRAWDOWN_THRESHOLD`
- `EmotionalState`: controlled educational categories such as `CALM`, `CONFIDENT`, `ANXIOUS`, `FOMO`, `REVENGE_TRADING`, `UNCERTAIN`
- `MistakeCategory`: `FOMO`, `REVENGE_TRADING`, `OVERSIZED_POSITION`, `IGNORED_STOP_LOSS`, `PREMATURE_EXIT`, `NO_CLEAR_THESIS`, `OVERTRADING`
- `Strategy`: a small controlled set, or a validated user-entered label if the product must remain flexible

Persist enum names, not ordinal values, so adding an enum value does not corrupt existing records.

## 9. Auditability and transaction boundaries

The following operations must be service-level database transactions:

### Execute buy order

1. Lock/load account cash.
2. Load and validate security price.
3. Calculate charges.
4. Validate sufficient cash.
5. Transition order to executed or rejected.
6. Update cash.
7. Create/update position.
8. Insert immutable transaction.
9. Update account aggregates.
10. Capture a snapshot when required.

### Execute sell order

1. Lock/load account and position.
2. Validate available quantity.
3. Calculate cost and proceeds.
4. Update or remove position.
5. Update cash and realized P&L.
6. Transition order.
7. Insert immutable transaction.
8. Record any alert/snapshot.

### Cancel order

Transition only eligible `CREATED` or `PENDING` orders to `CANCELLED`; do not alter cash or positions.

All financial records need server-generated timestamps. The order and transaction ledger should be append-oriented; corrections should be compensating records rather than destructive edits. Use row locking or optimistic versioning to prevent two simultaneous orders from spending the same cash.

## 10. Flyway recommendation

Do not install or configure Flyway in Phase 3A. The recommendation for the implementation phase is:

1. Introduce Flyway before the first production-like domain migration.
2. Baseline the current PostgreSQL schema and record the existing tables/data state.
3. Add versioned migrations for new columns/tables and data backfills.
4. Keep `ddl-auto=validate` (or `none`) outside a disposable local profile after migrations are established.
5. Avoid mixing Hibernate `update` with Flyway-managed production schema changes.

Why Flyway is recommended:

- the model will add multiple tables and foreign keys;
- data-preserving backfills need explicit ordering and verification;
- schema history must be reproducible in CI and fresh environments;
- `ddl-auto=update` cannot reliably express safe data migration or rollback.

The first implementation should include a backup, a baseline migration, a forward migration, and a reconciliation report. No tool installation or schema change is part of this design phase.

## 11. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Existing historical transactions are insufficient to reconstruct cost basis | Run reconciliation first; preserve uncertain rows and flag them instead of fabricating P&L. |
| Renaming `Stock` to `Security` breaks existing APIs and foreign keys | Evolve the existing table first or use a compatibility view/mapping. |
| Hibernate `ddl-auto=update` creates uncontrolled schema drift | Introduce Flyway before production-like migrations and switch to validation. |
| Concurrent orders overspend cash | Lock account rows or use optimistic versioning in one transaction. |
| Changing cost formulas changes historical results | Store executed charge components and configuration version per transaction. |
| Deleting securities/accounts breaks financial history | Restrict deletion and use archival flags. |
| User migration credentials are unsafe | Do not commit demo passwords; require explicit environment configuration. |
| Storing dynamic risk previews creates stale analytics | Calculate previews dynamically and persist only reviewed plan snapshots or future audit assessments. |

## 12. Implementation order after approval

1. Back up and inspect the current PostgreSQL schema/data; produce a migration reconciliation report.
2. Decide whether to evolve `stock`/`portfolio` in place first or create compatibility `securities`/`positions` tables.
3. Introduce Flyway and baseline the existing schema.
4. Add user/account tables and the legacy account mapping without changing existing APIs.
5. Enrich the security table and migrate existing references.
6. Add positions and backfill average cost with verified reconciliation.
7. Extend the transaction ledger while preserving legacy rows.
8. Add orders, trade plans, journal, alerts, and snapshots.
9. Add DTOs/services and migrate behavior incrementally.
10. Retire compatibility tables/endpoints only after the complete flow is verified.

## 13. Decision requested

Before implementation, review and approve:

- retaining `stock` as the physical security table during the first migration versus creating `securities`;
- retaining `portfolio` temporarily versus copying to a new `positions` table;
- the cost-basis reconstruction method for existing transactions;
- the demo-user migration policy for existing account `#1`;
- introducing Flyway at the beginning of the implementation phase.

