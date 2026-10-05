# Brokerage Simulator

A paper-trading application with a Spring Boot and PostgreSQL backend and an
Angular frontend. Catalogue prices are simulated/placeholder values, not
external market data. No real money is involved.

## Stack

- Java 21 and Spring Boot 4.1.1 (Maven)
- Spring Security with signed JWTs stored in httpOnly cookies
- Angular 21
- PostgreSQL with Flyway migrations

## Local setup

Create a PostgreSQL database named `brokerage`. By default the local backend
profile connects to `jdbc:postgresql://localhost:5433/brokerage` as `postgres`.
Set these environment variables if your local database differs:

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | Spring profile. Non-local profiles require `JWT_SECRET`. |
| `DB_URL` | `jdbc:postgresql://localhost:5433/brokerage` | PostgreSQL JDBC URL. |
| `DB_USERNAME` | `postgres` | PostgreSQL user. |
| `DB_PASSWORD` | No documented default | PostgreSQL password. |
| `JWT_SECRET` | Local-profile development-only key; required elsewhere | HS256 signing key, at least 32 characters. |
| `JWT_EXPIRY_MINUTES` | `1440` | JWT and cookie lifetime in minutes. |
| `COOKIE_SECURE` | `false` | Set `true` when serving over HTTPS. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Comma-separated allowed browser origins. |
| `STARTING_CASH` | `100000.00` | Virtual cash credited during registration. |
| `ADMIN_EMAIL` | Unset | Optional initial administrator email. Must be set with `ADMIN_PASSWORD`. |
| `ADMIN_PASSWORD` | Unset | Optional initial administrator password. Used only when creating a new admin. |

Copy `backend/.env.example` as a reference for the variables. Do not commit real
credentials. The local JWT fallback is for development only; use a random,
private `JWT_SECRET` in every deployed environment.

From `backend/`, start the API:

```powershell
.\mvnw.cmd spring-boot:run
```

From `frontend/`, install dependencies and start Angular:

```powershell
npm ci
npm start
```

The backend listens on port **8081** and the Angular development server on port
**4200**. Flyway applies schema migrations at backend startup; the PostgreSQL
database itself must already exist.

## API

| Method | Path | Access |
|---|---|---|
| `POST` | `/api/auth/register` | Public |
| `POST` | `/api/auth/login` | Public |
| `POST` | `/api/auth/logout` | Signed in |
| `GET` | `/api/auth/me` | Signed in |
| `POST` | `/api/auth/change-password` | Signed in |
| `GET` | `/api/account` | Signed in; returns only the caller's trading account |
| `GET` | `/api/securities` | Public; search, sector filter, sorting, pagination |
| `GET` | `/api/securities/{id-or-symbol}` | Public |
| `GET` | `/api/admin/users` | Admin |

Register and login bodies are JSON. Registration expects `email`, `password`,
and `displayName`; login expects `email` and `password`. Passwords must be at
least 8 characters and include a letter and a digit. Registration creates the
user and trading account in one transaction and immediately signs the user in.
Passwords are BCrypt-hashed and are never returned by API responses.

Use a cookie jar when testing the API with curl:

```powershell
curl.exe -i -c cookies.txt -H "Content-Type: application/json" `
  -d '{"email":"trader@example.test","password":"paper1234","displayName":"Trader"}' `
  http://localhost:8081/api/auth/register

curl.exe -i -b cookies.txt http://localhost:8081/api/auth/me
curl.exe -i -b cookies.txt http://localhost:8081/api/account
curl.exe -i -b cookies.txt -X POST http://localhost:8081/api/auth/logout
```

The JWT cookie is httpOnly, `SameSite=Lax`, scoped to `/`, and expires with the
configured JWT lifetime. Logging out clears the browser cookie; tokens are
stateless and a copied token remains valid until its expiry. The current
implementation does not maintain a token revocation list.

### CSRF and login throttling

The API uses an httpOnly cookie with `SameSite=Lax`, which prevents the cookie from being sent on most cross-site requests. State-changing endpoints use JSON request bodies and POST methods; there are no state-changing GET endpoints, and cross-origin browser access is restricted to configured CORS origins with credentials. For this portfolio paper-trading project, those restrictions are the selected CSRF mitigation rather than a synchronizer-token flow. The login limiter permits five failed attempts per email-and-IP key within 15 minutes and returns 429 with a `Retry-After` header on the next attempt; it is basic, in-memory, and resets on application restart.

## Tests

From `backend/`:

```powershell
.\mvnw.cmd clean test
```

From `frontend/`:

```powershell
npm test -- --watch=false
npm run build
```

## Current scope and limitations

- Authentication, authorization, account lookup, and a public simulated
  securities catalogue are implemented.
- Initial admin creation is optional via `ADMIN_EMAIL` and `ADMIN_PASSWORD`;
  credentials are not changed if that email already exists.
- Trading endpoints and frontend auth/pages are not yet implemented. The
  existing dashboard has not been migrated from the retired legacy API.
- Seeded Indian equity values are illustrative placeholders. There is no live
  market-data integration or price-update scheduler yet.
