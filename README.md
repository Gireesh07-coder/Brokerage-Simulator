# Brokerage Simulator

A paper-trading application with a Spring Boot and PostgreSQL backend and an
Angular frontend.

The API currently exposes a simulated-security catalogue at
`GET /api/securities` (search, sector filter, sorting, and pagination) and
`GET /api/securities/{id-or-symbol}`. Seeded Indian equity prices are
placeholders, not live market data.

Legacy account/trading endpoints have been retired as the backend moves to the
new domain model. Authentication and account-specific trading endpoints are
planned for later phases; the existing dashboard has not yet been migrated to
the new API.

## Stack

- Java 21 and Spring Boot 4.1.1 (Maven)
- Angular 21
- PostgreSQL with Flyway migrations

## Run locally

Start PostgreSQL and create the `brokerage` database. The backend's local
profile defaults to `jdbc:postgresql://localhost:5433/brokerage` with username
`postgres`. Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in the environment
when your local database uses different settings.

From `backend/`, start the API:

```powershell
.\mvnw.cmd spring-boot:run
```

From `frontend/`, install the locked dependencies and start Angular:

```powershell
npm ci
npm start
```

The frontend runs at `http://localhost:4200/`; the backend listens on
`http://localhost:8081/`.

Run backend tests from `backend/` with `.\mvnw.cmd test`. Run frontend tests
and a production build from `frontend/` with `npm test -- --watch=false` and
`npm run build`.
