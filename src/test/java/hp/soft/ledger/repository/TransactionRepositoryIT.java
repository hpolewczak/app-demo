package hp.soft.ledger.repository;

import hp.soft.BaseIntegrationTestIT;
import hp.soft.payment.dto.PaymentStatus;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static hp.soft.jooq.tables.Payments.PAYMENTS;

class TransactionRepositoryIT extends BaseIntegrationTestIT {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DSLContext dsl;

    @Test
    void insert_createsTransactionAndReturnsIt() {
        UUID paymentId = createPayment();
        BigDecimal amount = new BigDecimal("150.00");

        StepVerifier.create(transactionRepository.insert(paymentId, "Test transaction", amount))
                .expectNextMatches(tx ->
                        tx.id() != null
                                && tx.paymentId().equals(paymentId)
                                && tx.description().equals("Test transaction")
                                && tx.amount().compareTo(amount) == 0)
                .verifyComplete();
    }

    private UUID createPayment() {
        UUID paymentId = UUID.randomUUID();
        reactor.core.publisher.Mono.from(
                dsl.insertInto(PAYMENTS)
                        .set(PAYMENTS.PM_ID, paymentId)
                        .set(PAYMENTS.PM_CUSTOMER_ID, UUID.randomUUID())
                        .set(PAYMENTS.PM_MERCHANT_ID, UUID.randomUUID())
                        .set(PAYMENTS.PM_AMOUNT, new BigDecimal("150.00"))
                        .set(PAYMENTS.PM_STATUS, PaymentStatus.CREATED.name())
        ).block();
        return paymentId;
    }
}
