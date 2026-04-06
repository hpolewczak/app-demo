# Reactive BNPL Payment Ledger

A reactive Spring Boot application simulating Buy Now, Pay Later (BNPL) payments with double-entry bookkeeping.

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

## Architecture

- **Append-only ledger** — ledger lines are never updated or deleted; every event is a new record
- **Derived balances** — computed as `SUM(debit) - SUM(credit)` from ledger lines, no cached balance column
- **Full traceability** — every ledger line links to a transaction, every transaction links to a payment
- **Double-entry invariant** — `SUM(debits) = SUM(credits)` enforced by always writing entries in pairs

## JMeter Load Test

```bash
# 10 threads, 50 loops each
jmeter -n -t jmeter/payment-load-test.jmx -l jmeter/results/results.jtl

# 1 minute with 20 threads
jmeter -n -t jmeter/payment-load-test.jmx -JTHREADS=20 -JLOOPS=-1 -JRAMP_UP=5 \
  -Jjmeterengine.duration=60 -l jmeter/results/results.jtl

# GUI
jmeter -t jmeter/payment-load-test.jmx
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
