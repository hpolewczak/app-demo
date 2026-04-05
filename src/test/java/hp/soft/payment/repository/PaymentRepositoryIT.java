package hp.soft.payment.repository;

import hp.soft.BaseIntegrationTestIT;
import hp.soft.payment.dto.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.jooq.exception.IntegrityConstraintViolationException;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRepositoryIT extends BaseIntegrationTestIT {
    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void insert_createsPaymentWithCreatedStatus() {
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("200.00");

        StepVerifier.create(paymentRepository.insert(customerId, merchantId, amount))
                .assertNext(payment -> {
                    assertEquals(customerId, payment.customerId());
                    assertEquals(merchantId, payment.merchantId());
                    assertEquals(0, payment.amount().compareTo(amount));
                    assertEquals(PaymentStatus.CREATED, payment.status());
                })
                .verifyComplete();
    }

    @Test
    void findById_returnsPayment() {
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        StepVerifier.create(
                        paymentRepository.insert(customerId, merchantId, new BigDecimal("100.00"))
                                .flatMap(p -> paymentRepository.findById(p.id()))
                )
                .expectNextMatches(p -> p.customerId().equals(customerId) && p.merchantId().equals(merchantId))
                .verifyComplete();
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        StepVerifier.create(paymentRepository.findById(UUID.randomUUID()))
                .verifyComplete();
    }

    @Test
    void updateStatus_changesToPaid() {
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        StepVerifier.create(
                        paymentRepository.insert(customerId, merchantId, new BigDecimal("50.00"))
                                .flatMap(p -> paymentRepository.updateStatus(p.id(), PaymentStatus.PAID))
                )
                .assertNext(p -> assertEquals(PaymentStatus.PAID, p.status()))
                .verifyComplete();
    }

    @Test
    void findByIdForUpdate_returnsPayment() {
        StepVerifier.create(
                        paymentRepository.insert(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("75.00"))
                                .flatMap(p -> paymentRepository.findByIdForUpdate(p.id()))
                )
                .assertNext(p -> {
                    assertNotNull(p.id());
                    assertEquals(PaymentStatus.CREATED, p.status());
                })
                .verifyComplete();
    }

    @Test
    void findByIdForUpdate_returnsEmptyWhenNotFound() {
        StepVerifier.create(paymentRepository.findByIdForUpdate(UUID.randomUUID()))
                .verifyComplete();
    }

    @Test
    void updateStatusWithIdempotencyKey_setsKeyAndStatus() {
        String idempotencyKey = "idem-" + UUID.randomUUID();

        StepVerifier.create(
                        paymentRepository.insert(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("80.00"))
                                .flatMap(p -> paymentRepository.updateStatusWithIdempotencyKey(p.id(), PaymentStatus.PAID, idempotencyKey))
                )
                .assertNext(p -> {
                    assertEquals(PaymentStatus.PAID, p.status());
                    assertEquals(idempotencyKey, p.idempotencyKey());
                })
                .verifyComplete();
    }

    @Test
    void updateStatusWithIdempotencyKey_rejectsDuplicateKey() {
        String idempotencyKey = "idem-" + UUID.randomUUID();

        var firstPayment = paymentRepository.insert(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("60.00"))
                .flatMap(p -> paymentRepository.updateStatusWithIdempotencyKey(p.id(), PaymentStatus.PAID, idempotencyKey));

        var secondPayment = paymentRepository.insert(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("70.00"))
                .flatMap(p -> paymentRepository.updateStatusWithIdempotencyKey(p.id(), PaymentStatus.PAID, idempotencyKey));

        StepVerifier.create(firstPayment.then(secondPayment))
                .expectError(IntegrityConstraintViolationException.class)
                .verify();
    }
}
