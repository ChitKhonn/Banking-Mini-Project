# Banking API

Secure, modular REST API for a Banking Platform.
Java 17 + Spring Boot 3 + MongoDB + Redis + JWT (Spring Security).

## Stack

| Layer | Choice |
|---|---|
| Language / Framework | Java 17, Spring Boot 3.3 |
| Database | MongoDB (replica set required — used for multi-document transactions on Transfer) |
| Cache | Redis (accounts + transactions, per spec) |
| Auth | Spring Security + JWT (jjwt) |
| Docs | springdoc-openapi / Swagger UI |
| Async | `@Async` (mock email service, thread pool `emailTaskExecutor`) |
| Scheduling | `@Scheduled` cron job — deletes transactions older than 3 days |

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker (for local Mongo + Redis)

## Run locally

### 1. Start MongoDB (replica set) + Redis

```bash
docker compose up -d
```

This starts a **single-node MongoDB replica set** (`rs0`) — required because MongoDB
only supports multi-document ACID transactions on a replica set, and Transfer needs
this to atomically debit one account and credit another.

Wait a few seconds for the healthcheck to auto-initiate the replica set, then confirm:

```bash
docker exec -it banking-mongo mongosh --eval "rs.status()"
```

### 2. Run the app

```bash
mvn spring-boot:run
```

App starts on `http://localhost:8080`.

### 3. Swagger UI

```
http://localhost:8080/swagger-ui.html
```

## Seeded Admin

On first startup, an initial ADMIN is seeded automatically (since `Register New User`
is ADMIN-only, someone needs to exist first):

```
email:    admin@bank.com
password: Admin@12345
```

**Change this password after first login** — it's hardcoded in `DataSeeder` for
local/dev convenience only.

## Auth Flow

1. `POST /api/v1/auth/login` (public) → returns JWT
2. Pass the JWT as `Authorization: Bearer <token>` on all other endpoints
3. `POST /api/v1/auth/register` (ADMIN only) → create USER or ADMIN accounts

## Key Endpoints

| Method | Path | Role | Notes |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Public | Returns JWT |
| POST | `/api/v1/auth/register` | ADMIN | Create user |
| GET | `/api/v1/customers/{id}` | USER/ADMIN | Own data only unless ADMIN |
| GET | `/api/v1/customers` | ADMIN | List all |
| PUT | `/api/v1/customers/{id}` | USER/ADMIN | Own data only unless ADMIN |
| DELETE | `/api/v1/customers/{id}` | ADMIN | Strictly ADMIN-only, no self-delete |
| POST | `/api/v1/accounts` | ADMIN | Create account |
| GET | `/api/v1/accounts/{id}` | USER/ADMIN | Own account only unless ADMIN |
| GET | `/api/v1/accounts/customer/{userId}` | USER/ADMIN | Own accounts only unless ADMIN |
| PATCH | `/api/v1/accounts/{id}/status` | USER/ADMIN | Own account only unless ADMIN |
| DELETE | `/api/v1/accounts/{id}` | ADMIN | Soft-close account |
| POST | `/api/v1/accounts/{id}/deposit` | USER | Own account only |
| POST | `/api/v1/accounts/{id}/withdraw` | USER | Own account only |
| POST | `/api/v1/transfers` | USER | `fromAccountId` must be own; `toAccountId` can be anyone's |
| GET | `/api/v1/accounts/{id}/transactions` | USER/ADMIN | Own account only unless ADMIN |

## Design Decisions (confirmed during planning)

- **User = Customer**: merged into one `users` collection; `role` field (`ADMIN`/`USER`) distinguishes them.
- **JWT**: expiry configurable via `application.yml` (`jwt.expiration-ms`), no refresh token.
- **Transfer scope**: same-bank/system only, no external transfers.
- **Atomicity**: Transfer uses a real MongoDB multi-document transaction (`@Transactional` + `MongoTransactionManager`), not a simplified sequential update.
- **Insufficient balance**: throws `InsufficientBalanceException` → HTTP 400. No overdraft.
- **Email**: mocked/logged only (`EmailService` logs instead of sending real SMTP). Fires on **all** transaction types (Deposit, Withdraw, Transfer) on **SUCCESS only**; Transfer notifies **both** sender and receiver. "Update" (status change/reversal) also triggers a notification.
- **Redis**: caches accounts (`accounts`, `accountsByUser`) and transactions (`transactionsByAccount`); evicted explicitly on every write.
- **Account number format**: `{bankCode}-{branchCode}-{sequence}`, e.g. `BNK-001-00000123`.
- **Old transaction cleanup**: `@Scheduled` cron job (daily at midnight, configurable), not a Mongo TTL index — gives explicit control/logging.
- **Soft delete**: `status` field (`ACTIVE`/`DELETED` on users, `ACTIVE`/`CLOSED` on accounts) instead of hard delete, to preserve audit trail.
- **Optimistic locking**: `@Version` field on `Account`; `@Retryable` auto-retries on `OptimisticLockingFailureException` (3 attempts, 100ms backoff).
- **Delete Customer**: strictly ADMIN-only — a USER cannot self-delete.

## Project Structure

```
src/main/java/com/bank/api/
├── config/          # Security, Redis, Mongo, Async, Swagger, DataSeeder
├── controller/       # AuthController, UserController, AccountController, TransactionController
├── dto/
│   ├── request/
│   └── response/
├── entity/           # User, Account, Transaction
├── enums/
├── exception/         # Custom exceptions + GlobalExceptionHandler
├── repository/        # Spring Data MongoDB repositories
├── scheduler/         # TransactionCleanupScheduler (3-day retention)
├── security/          # JwtService, JwtAuthenticationFilter, UserPrincipal, CustomUserDetailsService
└── service/           # AuthService, UserService, AccountService, TransactionService, EmailService
```

