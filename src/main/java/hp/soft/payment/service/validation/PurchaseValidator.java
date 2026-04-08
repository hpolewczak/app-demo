package hp.soft.payment.service.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseValidator {
    private static final Duration VALIDATION_TIMEOUT = Duration.ofMillis(500);
    private final CreditCheckService creditCheckService;
    private final FraudCheckService fraudCheckService;

    public Mono<Void> validate(UUID customerId, UUID merchantId, BigDecimal amount) {
        return Mono.when(
                        creditCheckService.check(customerId, amount),
                        fraudCheckService.check(customerId, merchantId, amount)
                ).timeout(VALIDATION_TIMEOUT)
                .onErrorResume(TimeoutException.class, e -> {
                    log.warn("Validation timed out for customer={}, merchant={}, amount={} — proceeding with fail-open",
                            customerId, merchantId, amount);
                    return Mono.empty();
                });
    }
}
