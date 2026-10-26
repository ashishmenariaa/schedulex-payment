# ScheduleX — Payment Retry System

[![CI](https://github.com/ashishmenariaa/schedulex-payment/actions/workflows/ci.yml/badge.svg)](https://github.com/ashishmenariaa/schedulex-payment/actions/workflows/ci.yml)

A Spring Boot backend that automatically retries failed payments on a schedule, driven by a custom multi-threaded job scheduler. It guarantees that an order is never charged twice, even when two retries race each other.

**Stack:** Java 17 · Spring Boot 3.4 · Spring Data JPA / Hibernate · MySQL · JUnit 5 · Mockito · H2 (tests) · GitHub Actions · Docker

## The problem

Payments fail for temporary reasons: insufficient funds, a bank timeout, an expired card. Retrying later recovers many of them, but retry logic is easy to get wrong. A careless retry system can charge the customer twice, retry an order that was already paid, or lose scheduled retries when the server restarts. This project handles each of those cases explicitly.

## How it works

```
POST /api/orders ──► OrderService.createOrder ──► charge (attempt 1)
                                                     │
                              success ◄──────────────┴──────────► failure
                                 │                                   │
                            order PAID                 retryCount++, nextRetryTime = now + backoff
                                                       create Job("Payment Retry - ORD_x", priority 8)
                                                                     │
    ┌────────────────────────── ScheduleX engine ──────────────────────┴──┐
    │ JobSchedulerEngine.pollDueJobs (every 5s)                            │
    │   claimDueJobs(): PENDING → QUEUED in one transaction                │
    │        ▼                                                             │
    │ JobQueueManager: PriorityBlockingQueue (payments jump the queue)     │
    │        ▼                                                             │
    │ consumer thread ──► JobExecutor ──► worker pool (10 threads)         │
    │                                       └─► OrderService.retryIfDue    │
    └──────────────────────────────────────────────────────────────────────┘
```

**Retry schedule:** attempt 1 happens immediately, attempt 2 after 6 hours, attempt 3 after 24 more hours. If all three fail, the order is cancelled. A 72-hour step is used if `maxRetries` is raised above 3.

## Correctness guarantees (and the tests that prove them)

| Guarantee | How | Test |
|---|---|---|
| An order is never charged twice concurrently | `@Version` optimistic lock on `Order`; the `PROCESSING` status is flushed **before** the gateway is called, so a racing request fails before charging | `concurrentRetriesOfTheSameOrderChargeOnlyOnce` |
| A paid, in-flight or cancelled order can't be charged | `Order.isPayable()` guard → HTTP 409 | `paidOrderIsNeverChargedAgain`, `retryingPaidOrderReturns409` |
| The same attempt is never recorded twice | Unique constraint on `(orderId, attemptNumber)` | enforced by schema |
| A stale retry job doesn't fire early | `retryIfDue` checks `nextRetryTime` before charging | `retryJobFiringBeforeNextRetryTimeIsSkipped` |
| A due job is queued only once | `claimDueJobs` moves jobs `PENDING → QUEUED` atomically | `claimedJobsAreNotClaimedAgain` |
| Jobs survive a restart | On startup, jobs left `QUEUED` go back to `PENDING` | `JobService.requeueOrphanedJobs` |

There are 25 tests in total (unit, `@WebMvcTest` and `@SpringBootTest` on H2), and CI runs them on every push.

## Design decisions

- **Payment gateway behind an interface.** `PaymentGateway` is implemented by `SimulatedPaymentGateway`: the first attempt always fails, the 2nd succeeds 70% of the time, later attempts 90%. A Razorpay or Stripe client would implement the same interface with no change to the business logic. Tests mock it for deterministic results.
- **One retry path.** Retries happen only through scheduled jobs. An earlier version also had a separate order poller, which could fire alongside the job and charge twice. It was removed.
- **Custom scheduler instead of Quartz.** It was built to understand the internals: a producer/consumer queue, a priority ordering and a bounded thread pool. The pool size comes from `schedulex.worker.threads`, and the pool shuts down cleanly with the app.
- **DTOs with validation.** Clients can't set server-controlled fields like `paymentStatus` or `retryCount`. Errors return a consistent JSON body (400, 404 or 409).
- **Known trade-off.** The DB transaction stays open during the gateway call. That's fine at this scale; at higher volume I'd claim the order in a short transaction and record the result in a second one.

## Run it

**With Docker:**
```bash
DB_PASSWORD=secret docker compose up --build
```

**Locally** (Java 17+, MySQL 8 running):
```bash
cd ScheduleX
export DB_USERNAME=root DB_PASSWORD=yourpassword   # Windows: set DB_PASSWORD=...
./mvnw spring-boot:run
```
Open the dashboard at http://localhost:8080. The database `schedulex_db` is created automatically.

**Tests** (no database needed):
```bash
cd ScheduleX && ./mvnw test
```

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/orders` | Create order and attempt first payment (201) |
| GET | `/api/orders` | List orders |
| GET | `/api/orders/{orderId}` | Get order (404 if unknown) |
| POST | `/api/orders/{orderId}/retry` | Manual retry (409 if paid, in progress or cancelled) |
| GET | `/api/orders/{orderId}/transactions` | Payment attempt history |
| GET | `/api/orders/stats` | Order counts by payment status |
| POST | `/api/jobs` | Schedule a generic job |
| GET | `/api/jobs`, `/api/jobs/{id}`, `/api/jobs/stats` | Inspect jobs |

```bash
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d '{
  "customerId": "CUST_1", "customerName": "Asha Verma", "customerEmail": "asha@example.com",
  "customerPhone": "9876543210", "amount": 1299.00
}'
```

## Project structure

```
ScheduleX/src/main/java/com/schedulex
├── controller/   REST endpoints
├── service/      OrderService (retry rules), PaymentService (attempt recording, backoff), JobService
├── scheduler/    JobSchedulerEngine (poller + consumer), JobQueueManager, JobExecutor
├── gateway/      PaymentGateway interface + simulator
├── model/        JPA entities and enums
├── dto/          Validated request records
├── exception/    Custom exceptions + global handler
└── config/       Worker pool, scheduling
```

## Roadmap

- Razorpay integration with webhook verification
- Email notifications on payment success or cancellation
- Redis-backed queue so multiple instances can share work
- Spring Security for the admin endpoints

## Author

**Ashish Menaria**: [GitHub](https://github.com/ashishmenariaa) · [LinkedIn](https://www.linkedin.com/in/ashish-menaria-6593511a7/)
