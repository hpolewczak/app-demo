# Reactive BNPL Payment Ledger

A reactive Spring Boot application simulating Buy Now, Pay Later (BNPL) payments with double-entry bookkeeping.

> **Note:** This is a demo project. Each purchase executes up to 10 writes in a single DB transaction
> (account creation + payment + 2 transactions + 4 ledger lines), creating a connection pool bottleneck under load.
> To scale, ledger writes could be decoupled via a lightweight **outbox pattern** (poller-based, no new infra)
> or a production-grade **Kafka**-based event pipeline with backpressure and horizontal scaling.

## Tech Stack

Java 25, Spring Boot 4.0.5, WebFlux, PostgreSQL 18 + R2DBC, jOOQ, Flyway, Testcontainers, JMeter

## Quick Start

```bash
docker compose up -d
export JAVA_HOME=/Users/hadek/Library/Java/JavaVirtualMachines/openjdk-25/Contents/Home
./mvnw clean compile
./mvnw spring-boot:run          # http://localhost:8080
./mvnw test                     # unit tests
./mvnw verify                   # unit + integration tests (requires Docker)
```

## How It Works

Three accounts model the BNPL lifecycle:

| Account | Type | Purpose |
|---|---|---|
| **ZILCH_CASH** | Asset | Zilch's cash pool (seeded at startup) |
| **CUSTOMER_RECEIVABLE** | Asset | Money owed by a customer |
| **MERCHANT_PAYABLE** | Liability | Money owed to a merchant |

Customer and merchant accounts are created on-the-fly when first seen (SELECT-first, INSERT ON CONFLICT pattern).

### Double-Entry Rules

Each ledger entry has a **debit (DR)** and **credit (CR)** side. The effect depends on the account type:

| | DR (debit) | CR (credit) |
|---|---|---|
| **Asset** (ZILCH_CASH, CUSTOMER_RECEIVABLE) | increases balance | decreases balance |
| **Liability** (MERCHANT_PAYABLE) | decreases balance | increases balance |

The fundamental invariant: **Assets = Liabilities + Equity**. Debits increase the left side, credits increase the right side.

### Purchase (100.00)

```
  CUSTOMER                ZILCH                 MERCHANT
     |                      |                      |
     |   1. Obligation      |                      |
     |  ◄───── 100 ─────   |                      |
     |  (receivable created)|                      |
     |                      |   2. Settlement       |
     |                      |  ───── 100 ─────►    |
     |                      |  (cash paid out)      |
```
```
DR CUSTOMER_RECEIVABLE 100    CR MERCHANT_PAYABLE  100
DR MERCHANT_PAYABLE    100    CR ZILCH_CASH        100
```

**Line 1** — CUSTOMER_RECEIVABLE (asset) debited: balance **+100** (customer owes us more). MERCHANT_PAYABLE (liability) credited: balance **+100** (we owe merchant more).

**Line 2** — MERCHANT_PAYABLE (liability) debited: balance **-100** (obligation cleared). ZILCH_CASH (asset) credited: balance **-100** (cash paid out).

### Pay-off

```
  CUSTOMER                ZILCH
     |                      |
     |   3. Repayment       |
     |  ───── 100 ─────►   |
     |  (debt cleared)      |
```
```
DR ZILCH_CASH          100    CR CUSTOMER_RECEIVABLE 100
```

**Pay-off** — ZILCH_CASH (asset) debited: balance **+100** (cash received). CUSTOMER_RECEIVABLE (asset) credited: balance **-100** (customer debt cleared).

After the full cycle, all balances return to zero.

## API

```bash
# Purchase
curl -X POST http://localhost:8080/api/v1/payments/purchase \
  -H "Content-Type: application/json" \
  -d '{"customerId":"<uuid>","merchantId":"<uuid>","amount":100.00}'

# Pay Off
curl -X POST http://localhost:8080/api/v1/payments/pay-off \
  -H "Content-Type: application/json" \
  -d '{"paymentId":"<id-from-purchase>","idempotencyKey":"unique-key-1"}'

# Payment Detail (payment + transactions + ledger lines)
curl http://localhost:8080/api/v1/payments/{payment-id}

# Trial Balance (SUM(debits) = SUM(credits))
curl http://localhost:8080/api/v1/report/trial-balance
```

## Database Schema

```
┌──────────────────────────────────┐
│            accounts              │
├──────────────────────────────────┤
│ acc_id          UUID        [PK] │
│ acc_code        VARCHAR(50)      │
│ acc_name        VARCHAR(255)     │
│ acc_type        VARCHAR(20)      │  CHECK: ASSET | LIABILITY
│ acc_customer_id UUID             │
│ acc_merchant_id UUID             │
├──────────────────────────────────┤
│ UNIQUE(acc_code, acc_customer_id,│
│        acc_merchant_id)          │
└──────────────────────────────────┘
              ▲
              │ ll_acc_id (FK)
              │
┌──────────────────────────────────┐       ┌──────────────────────────────────┐
│          ledger_lines            │       │          transactions            │
├──────────────────────────────────┤       ├──────────────────────────────────┤
│ ll_id       UUID          [PK]  │       │ tx_id          UUID        [PK] │
│ ll_tx_id    UUID          [FK]──┼──────>│ tx_description VARCHAR(255)     │
│ ll_acc_id   UUID          [FK]──┼───▲   │ tx_pm_id       UUID        [FK]─┼──┐
│ ll_debit    NUMERIC(19,4)       │       │ tx_amount      NUMERIC(19,4)    │  │
│ ll_credit   NUMERIC(19,4)       │       │ tx_created_at  TIMESTAMPTZ      │  │
│ ll_created_at TIMESTAMPTZ       │       └──────────────────────────────────┘  │
├──────────────────────────────────┤                                            │
│ CHECK: exactly one of            │       ┌──────────────────────────────────┐  │
│   debit/credit is NOT NULL       │       │            payments              │  │
└──────────────────────────────────┘       ├──────────────────────────────────┤  │
                                           │ pm_id             UUID     [PK] │<─┘
                                           │ pm_customer_id    UUID          │
                                           │ pm_merchant_id    UUID          │
                                           │ pm_amount         NUMERIC(19,4) │
                                           │ pm_status         VARCHAR(20)   │
                                           │ pm_idempotency_key VARCHAR(255) │  UNIQUE
                                           │ pm_created_at     TIMESTAMPTZ   │
                                           └──────────────────────────────────┘
```

- `payments` 1:N `transactions` — purchase + pay-off transactions per payment
- `transactions` 1:N `ledger_lines` — each transaction has multiple DR/CR entries
- `accounts` 1:N `ledger_lines` — each line debits or credits one account

## Architecture

- **Append-only ledger** — ledger lines are never updated or deleted; every event is a new record
- **Derived balances** — computed as `SUM(debit) - SUM(credit)` from ledger lines, no cached balance column
- **Full traceability** — every ledger line links to a transaction, every transaction links to a payment
- **Double-entry invariant** — `SUM(debits) = SUM(credits)` enforced by always writing entries in pairs

## JMeter Load Test

```bash
# GUI
jmeter -t jmeter/payment-load-test.jmx

# Default: 100 requests/sec                                                                                                                  
jmeter -n -t jmeter/payment-load-test.jmx -l jmeter/results/results.jtl

# Limit to 10 requests/sec (600/min)
jmeter -n -t jmeter/payment-load-test.jmx -JTPM=600 -l jmeter/results/results.jtl
                                                                                                                                                 
# Limit to 50 requests/sec (3000/min)
jmeter -n -t jmeter/payment-load-test.jmx -JTPM=3000 -l jmeter/results/results.jtl
```

## Project Structure

```
src/main/java/hp/soft/
  account/    # Account entities, enums, repository, service
  ledger/     # Ledger lines, transactions, services
  payment/    # Payment orchestration, controller, DTOs
  report/     # Trial balance reporting
  config/     # jOOQ, Flyway, exception handler
```
