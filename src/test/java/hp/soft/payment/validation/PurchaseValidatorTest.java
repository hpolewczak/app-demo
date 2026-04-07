package hp.soft.payment.validation;

import hp.soft.payment.service.validation.CreditCheckService;
import hp.soft.payment.service.validation.FraudCheckService;
import hp.soft.payment.service.validation.PurchaseValidator;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurchaseValidatorTest {
    private final CreditCheckService creditCheckService = mock(CreditCheckService.class);
    private final FraudCheckService fraudCheckService = mock(FraudCheckService.class);
    private final PurchaseValidator purchaseValidator = new PurchaseValidator(creditCheckService, fraudCheckService);

    private final UUID customerId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("100.00");

    @Test
    void validate_passesWhenBothChecksPass() {
        when(creditCheckService.check(customerId, amount)).thenReturn(Mono.empty());
        when(fraudCheckService.check(customerId, merchantId, amount)).thenReturn(Mono.empty());

        StepVerifier.create(purchaseValidator.validate(customerId, merchantId, amount))
                .verifyComplete();
    }

    @Test
    void validate_failsWhenCreditCheckFails() {
        when(creditCheckService.check(customerId, amount))
                .thenReturn(Mono.error(new IllegalArgumentException("Credit check failed")));
        when(fraudCheckService.check(customerId, merchantId, amount)).thenReturn(Mono.empty());

        StepVerifier.create(purchaseValidator.validate(customerId, merchantId, amount))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("Credit check failed"))
                .verify();
    }

    @Test
    void validate_failsWhenFraudCheckFails() {
        when(creditCheckService.check(customerId, amount)).thenReturn(Mono.empty());
        when(fraudCheckService.check(customerId, merchantId, amount))
                .thenReturn(Mono.error(new IllegalArgumentException("Fraud check failed")));

        StepVerifier.create(purchaseValidator.validate(customerId, merchantId, amount))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("Fraud check failed"))
                .verify();
    }

    @Test
    void validate_proceedsWhenChecksTimeout() {
        when(creditCheckService.check(customerId, amount))
                .thenReturn(Mono.<Void>empty().delaySubscription(Duration.ofSeconds(2)));
        when(fraudCheckService.check(customerId, merchantId, amount))
                .thenReturn(Mono.<Void>empty().delaySubscription(Duration.ofSeconds(2)));

        StepVerifier.create(purchaseValidator.validate(customerId, merchantId, amount))
                .verifyComplete();
    }
}
