package hp.soft.payment.repository;

import hp.soft.payment.dto.Payment;
import hp.soft.payment.dto.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static hp.soft.jooq.tables.Payments.PAYMENTS;

@Repository
@RequiredArgsConstructor
public class PaymentRepository {
    private final DSLContext dsl;

    public Mono<Payment> insert(UUID customerId, UUID merchantId, BigDecimal amount) {
        UUID id = UUID.randomUUID();
        return Mono.from(
                dsl.insertInto(PAYMENTS)
                        .set(PAYMENTS.PM_ID, id)
                        .set(PAYMENTS.PM_CUSTOMER_ID, customerId)
                        .set(PAYMENTS.PM_MERCHANT_ID, merchantId)
                        .set(PAYMENTS.PM_AMOUNT, amount)
                        .set(PAYMENTS.PM_STATUS, PaymentStatus.CREATED.name())
        ).then(findById(id));
    }

    public Mono<Payment> findById(UUID id) {
        return Mono.from(
                dsl.selectFrom(PAYMENTS)
                        .where(PAYMENTS.PM_ID.eq(id))
        ).map(this::toPayment);
    }

    public Mono<Payment> findByIdForUpdate(UUID id) {
        return Mono.from(
                dsl.selectFrom(PAYMENTS)
                        .where(PAYMENTS.PM_ID.eq(id))
                        .forUpdate()
        ).map(this::toPayment);
    }

    public Mono<Payment> updateStatusWithIdempotencyKey(UUID id, PaymentStatus status, String idempotencyKey) {
        return Mono.from(
                dsl.update(PAYMENTS)
                        .set(PAYMENTS.PM_STATUS, status.name())
                        .set(PAYMENTS.PM_IDEMPOTENCY_KEY, idempotencyKey)
                        .where(PAYMENTS.PM_ID.eq(id))
        ).then(findById(id));
    }

    public Mono<Payment> updateStatus(UUID id, PaymentStatus status) {
        return Mono.from(
                dsl.update(PAYMENTS)
                        .set(PAYMENTS.PM_STATUS, status.name())
                        .where(PAYMENTS.PM_ID.eq(id))
        ).then(findById(id));
    }

    private Payment toPayment(hp.soft.jooq.tables.records.PaymentsRecord r) {
        return Payment.builder()
                .id(r.getPmId())
                .customerId(r.getPmCustomerId())
                .merchantId(r.getPmMerchantId())
                .amount(r.getPmAmount())
                .status(PaymentStatus.valueOf(r.getPmStatus()))
                .idempotencyKey(r.getPmIdempotencyKey())
                .createdAt(r.getPmCreatedAt())
                .build();
    }
}
