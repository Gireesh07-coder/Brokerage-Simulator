# TradeGuard — Phase 1 Project Audit

**Date:** 2026-09-23  
**Repository:** `Brokerage-Simulator`  
**Git:** `main` tracking `origin/main` (1 commit: `Initial brokerage simulator setup`). Most trading/portfolio/UI work is **uncommitted**.  
**Scope of this document:** inspection only. No product features were implemented in this phase.

---

## 1. Current architecture

```
Angular 21 (standalone App component, no routes)
        HTTP (hardcoded http://localhost:8081)
        ↓
Spring Boot 4.1.1 REST API  (com.gireesh.brokerage, Java 21 target; local JDK 25)
        JPA / Hibernate ddl-auto=update
        ↓
PostgreSQL  (jdbc:postgresql://localhost:5433/brokerage)
```

This is already a **modular monolith**. That is the right shape for the MVP. Do not split into microservices.

### Frontend

| Item | Finding |
|------|---------|
| Framework | Angular 21.2, TypeScript 5.9, Vitest |
| Structure | Single root component (`app.ts` / `app.html` / `app.scss`) |
| Routing | `app.routes.ts` is empty; `provideRouter` is wired but unused |
| Services | `AccountService`, `StockService`, `TradingService` |
| Pages | None. Dashboard, market watch, order ticket, holdings, and transactions live on one screen |
| Charts | None |
| Auth UI | None |

### Backend

Layered as Controller → Service → Repository, which should be **kept**.

| Layer | What exists |
|-------|-------------|
| Controllers | `AccountController`, `StockController`, `TradingController`, `PortfolioController`, `TransactionController` |
| Services | `AccountService`, `StockService`, `TradingService`, `PortfolioService`, `TransactionService` |
| Repositories | JPA for Account, Stock, Portfolio, Transaction |
| Entities | `Account`, `Stock`, `Portfolio`, `Transaction`, `TransactionType` |
| Other | `CorsConfig`, `DataInitializer` (seed US symbols), `GlobalExceptionHandler` (AccountNotFound only) |
| Auth | **None** — no Spring Security, no User, no JWT |
| DTOs | **None** — entities are returned directly |
| Market data | Price stored on `Stock.currentPrice`; no `MarketDataService` |
| Orders | Immediate buy/sell; no Order entity or state machine |

### Database (Hibernate-managed, no Flyway/Liquibase)

Local DB is live and already contains demo paper-trading data.

**Account** — `id`, `name`, `cashBalance`  
**Stock** — `id`, `symbol`, `companyName`, `currentPrice` (no unique symbol, no exchange/sector/previousClose/volume)  
**Portfolio** (position) — `id`, `account`, `stock`, `quantity` (no average entry price)  
**transactions** — `id`, `account`, `stock`, `type` (BUY/SELL), `quantity`, `price`, `tradeValue`, `brokerage`, `totalAmount`, `transactionTime`

Missing for TradeGuard: User, TradingAccount fields (realized/unrealized P&L, invested capital), Order, Trade, TradePlan, RiskAssessment, JournalEntry, Alert, PortfolioSnapshot, cost-component breakdown.

### Runtime verified in this audit

| Check | Result |
|-------|--------|
| Backend compile (`mvnw compile`) | Success |
| PostgreSQL `localhost:5433` | Open |
| `spring-boot:run` | Started (`Started BrokerageApiApplication`) |
| `GET /api/stocks` | 5 US stocks seeded |
| `GET /api/accounts/1` | Account `Gireesh`, cash ≈ ₹1,05,392.99 |
| `GET /api/portfolio/account/1` | 5 holdings (AAPL, TSLA, AMZN, MSFT, GOOGL) |
| `GET /api/transactions/account/1` | At least one BUY (GOOGL) |
| `GET /api/accounts/9999` | **500** (should be 404) |
| Buy qty `0` / huge qty | **500** (should be 400 with a business code) |
| Frontend / Docker | Angular not started this session. Docker CLI is **not installed** |
| `application-local.properties` | Present locally, gitignored (good). Contains a local DB password |

Frontend was not started (`4200` closed). The UI is a single SPA component; it should run with `npm start` once the API is up.

---

## 2. Existing features (what already works)

These are real and should be **reused**, not rewritten:

1. **Virtual cash account** — create, get, deposit; cash is decremented/incremented on trade.
2. **Security master (minimal)** — seed + list + get by id.
3. **Immediate MARKET buy/sell** — `@Transactional`; insufficient cash and oversell quantity are rejected (as exceptions).
4. **Holdings update** — add quantity on buy; subtract on sell.
5. **Trade ledger** — BUY/SELL rows with trade value, a single brokerage field, total, timestamp.
6. **Brokerage estimate** — 0.1% of trade value (backend + frontend). Educational, not a full Indian cost model.
7. **CORS** for `http://localhost:4200`.
8. **Paper-trading UI shell** — sidebar, summary cards, market list, order ticket, holdings table, transaction table, empty states, toasts, loading/trading flags, INR formatting. Visual direction is closer to a fintech demo than a school CRUD form.
9. **Add-funds modal logic** exists in TypeScript (API works), but there is **no button in the template** that calls `openAddFunds()`.

---

## 3. What is broken or incomplete

### Runtime / correctness

| Issue | Why it matters |
|-------|----------------|
| `AccountService.getAccount` throws `RuntimeException`, not `AccountNotFoundException` | Missing account returns 500, not 404 |
| Trading/validation errors are untyped `RuntimeException` | Frontend gets 500; no `INSUFFICIENT_FUNDS` payload |
| Sell does not delete quantity-0 rows | Closed positions stay as empty holdings |
| No average cost / realized P&L | Cannot compute TradeGuard P&L correctly |
| Frontend hard-codes `accountId = 1` | Second user / register flow is impossible |
| NSE clock gate in the UI | After 15:30 IST, Buy/Sell is disabled even though this is paper trading and the API still accepts orders |
| Daily change always `+0.00%` | Fake market UI |
| `account.spec.ts` injects `Account` instead of `AccountService` | Unit test is invalid |
| Context-load test needs a live Postgres | CI/local test is environment-coupled |

### Product gaps vs TradeGuard MVP

No authentication, JWT, or registration.  
No per-user ₹10,00,000 virtual account provisioning.  
No Indian NSE seed set (RELIANCE, TCS, …). Current seed is US mega-caps while the UI is INR/NSE.  
No order types (LIMIT, STOP-LOSS) or order states. Every fill is immediate MARKET.  
No configurable transaction-cost engine (STT, GST, stamp, SEBI, exchange).  
No trade plan, pre-trade risk analyzer, concentration, drawdown, journal, alerts, analytics, or market simulator.  
No DTOs, request bodies (query params only), or structured error JSON.  
No charts, no real routing, sidebar items are non-functional.

### Security

- No password hashing, JWT, or API authorization.
- Anyone who can hit `:8081` can create accounts, deposit unlimited virtual cash, and trade as `accountId=1`.
- Local DB password lives in `application-local.properties` (gitignored; keep it that way). Move remaining config to env vars for the documented runbook.

---

## 4. What should be reused

- Package layout and **layered Spring** style (`controller` / `service` / `repository` / `entity`).
- `TradingService` transactional buy/sell as the **starting point** of an order-execution engine (extend; do not throw away cash + position updates).
- `Transaction` as the cash-movement ledger (extend columns; do not replace with JSON blobs).
- `Portfolio` as the position table (add average price, invested value; rename conceptually to Position).
- `Stock` as Security (add fields; introduce `MarketDataService` in front of price reads).
- Angular services as HTTP clients (add interceptors later for JWT).
- Existing dashboard SCSS language (green/neutral fintech, empty states, order ticket) — extract into layout + feature pages rather than restyling from scratch.
- PostgreSQL + JPA. Keep it.

---

## 5. What should change (smallest correct direction)

**Do not** create a new repo or a second app. Evolve this one into TradeGuard.

| Change | Rationale |
|--------|-----------|
| Introduce `User` + JWT (Spring Security, minimal) | Required for protected APIs and per-user capital |
| Split `Account` into user-owned `TradingAccount` with full balances | Current account is only name + cash |
| Add `Order` state machine; keep MARKET path first | Spec requires PENDING/REJECTED/CANCELLED, not instant success |
| Extract `TransactionCostService` (config properties) | Replace hardcoded `0.001` |
| Add `averageEntryPrice` and realized P&L on sell | Core differentiator; current model cannot do this |
| `MarketDataService` + deterministic simulator | Prices must not be random per refresh; later swappable |
| DTOs + one `ApiException` hierarchy + error body `{status, error, message, timestamp}` | Current 500s are not client-usable |
| Angular routes + auth guard + feature components | Empty `routes` and fake sidebar |
| Replace US seed with labelled NSE paper universe | Matches product + INR UI |
| Flyway (or keep ddl-auto only in local profile) | Schema will grow; `update` is technical debt |
| Stop using NSE wall-clock to block paper orders (or make it a **setting**, default off) | Otherwise the demo dies every evening |
| **Defer AI journal assistant** | Spec: only after core MVP. No LLM required for v1 |

**Assumption (to confirm):** keep module name `brokerage-api` / package `com.gireesh.brokerage` internally; product name in UI/README becomes **TradeGuard**. Renaming Java packages is cosmetic and can wait.

**Assumption:** one rupee-denominated cash account per user; no multi-currency.

**Out of scope for MVP (will not build unless you insist):** Kafka, Redis, Kubernetes, microservices, real broker APIs, price prediction, WebSockets (nice later), options/F&O.

---

## 6. Technical debt

1. Entities exposed over REST (lazy/serialization and API stability risk).
2. Query-string mutating APIs (`POST /api/trading/buy?...`).
3. Duplicated CORS (`CorsConfig` + `@CrossOrigin` on some controllers).
4. No unique constraint on `Stock.symbol`.
5. No indexes on account/stock FKs (fine at tiny scale; add with schema).
6. Integer quantity; BigDecimal prices (keep BigDecimal; quantity as `int` is OK for MVP).
7. Frontend `ChangeDetectorRef` + `console.log` everywhere (zoneless/manual CD workaround).
8. No root README, no Docker Compose (Docker is also missing on this machine).
9. Git history does not contain the trading engine yet — high loss risk until committed.
10. `application.properties` activates `local` always; tests inherit live DB.

---

## 7. Proposed implementation order

Aligned with your phases. **Stop after each phase for review unless you approve a larger batch.**

| Phase | Work | Notes |
|-------|------|--------|
| **1** | This audit | Done |
| **2** | Fix runtime issues: typed exceptions → 4xx JSON; sell closes positions; env-based DB config; optional Docker Compose for Postgres; un-block paper trading after hours | Smallest correctness fixes before features |
| **3** | Schema: User, TradingAccount, Security, Order, Position, Transaction, TradePlan, RiskAssessment, JournalEntry, Alert, PortfolioSnapshot | Flyway preferred; migrate existing tables carefully (local data exists) |
| **4** | Register / login / logout, password hash, JWT, auto-provision ₹10,00,000 | Demo user documented, not committed as a real secret |
| **5** | NSE seed + `MarketDataService` + deterministic price-step simulator | Admin/dev endpoint to apply a price scenario for the demo |
| **6** | Order engine: MARKET first, then LIMIT/STOP validation + PENDING | Duplicate-execution guard |
| **7** | Positions, average cost, realized/unrealized P&L, snapshots | |
| **8** | Configurable `TransactionCostService` + pre-trade estimate | Labelled simulation |
| **9** | Trade plan + risk analyzer API (no buy/sell advice) | |
| **10** | Dashboard: account, P&L, concentration, drawdown, charts | Reuse current shell |
| **11** | Trading journal + post-close trade review | |
| **12** | Performance analytics (show “Not enough data”) | |
| **13** | Non-predictive alerts | |
| **14** | AI journal summary | **Skip until 1–13 work**; feature flag |
| **15** | Unit + integration tests listed in the spec | H2 or Testcontainers if we add Docker; otherwise `@DataJpaTest` + Testcontainers later |
| **16** | Routes, empty/loading/error, polish, settings | |
| **17** | README, `.env.example`, runbook | |

### Suggested first coding slice (Phase 2 only, after your approval)

1. Domain exceptions + `GlobalExceptionHandler` contract.  
2. Close-out of zero positions.  
3. Documented local run (Postgres URL via env).  
4. Do **not** yet rewrite the Angular app.

---

## 8. How to run / test what exists today

**Prerequisites observed on this machine:** JDK 25, Node 24, Postgres on port **5433**, database `brokerage`. Docker is not available.

```bash
# Backend (from Brokerage-Simulator/backend)
# Requires application-local.properties:
#   spring.datasource.url=jdbc:postgresql://localhost:5433/brokerage
#   spring.datasource.username=...
#   spring.datasource.password=...
.\mvnw.cmd spring-boot:run

# Frontend (from Brokerage-Simulator/frontend)
npm start
# open http://localhost:4200
```

API is on **8081**. Frontend assumes account **id=1** already exists.

```bash
# Tests today (limited)
.\mvnw.cmd test          # context load; needs Postgres
cd frontend && npm test  # account.spec is currently invalid
```

---

## 9. Remaining problems after Phase 1

- Product is a **single-account MARKET simulator**, not TradeGuard.
- Error contract is not API-safe (500s).
- Auth, risk, journal, analytics, orders, and market engine are absent.
- Local paper data (US names, ~₹1.05L cash, existing lots) will need a **migration or reset strategy** when ₹10L demo accounts and NSE symbols are introduced.
- Uncommitted code should be committed once you approve proceeding (logical commits per later phases).

## Phase 2 — Completed

Phase 2 foundation and correctness fixes are complete:

- Added typed domain exceptions and structured 4xx API error responses.
- Added a database-level unique constraint for `Stock.symbol`, plus a service precheck for duplicate symbols.
- Added focused exception handling tests for missing accounts.
- Added invalid quantity tests for zero and negative values.
- Added an insufficient-funds test verifying that cash, positions, and transactions are unchanged.
- Added a full-position closure test verifying transaction recording, cash update, and position deletion.
- Added a database constraint test proving duplicate stock symbols are rejected.
- Removed frontend paper-trading restrictions based on NSE market hours.
- Moved local database configuration to environment variables.
- Added an H2 test profile so focused and context tests do not require the live PostgreSQL database.

Final verification:

```text
.\mvnw.cmd test     PASS — 8 tests, 0 failures, 0 errors
.\mvnw.cmd compile  PASS
```

Phase 3 has not been started as part of this completion step.
