package hp.soft.payment.repository;

import hp.soft.BaseIntegrationTestIT;
import hp.soft.payment.dto.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
