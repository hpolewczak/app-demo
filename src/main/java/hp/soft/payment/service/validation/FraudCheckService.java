package hp.soft.payment.service.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class FraudCheckService {
    private static final BigDecimal FRAUD_THRESHOLD = new BigDecimal("50000.00");

    public Mono<Void> check(UUID customerId, UUID merchantId, BigDecimal amount) {
        return Mono.delay(Duration.ofMillis(80))
                .then(Mono.defer(() -> {
                    if (amount.compareTo(FRAUD_THRESHOLD) > 0) {
                        log.warn("Fraud check failed: suspicious amount {} for customer {} merchant {}", amount, customerId, merchantId);
                        return Mono.error(new IllegalArgumentException(
                                "Fraud check failed: suspicious amount %s for customer %s merchant %s"
                                        .formatted(amount, customerId, merchantId)));
                    }
                    log.debug("Fraud check passed for customer {} merchant {} amount {}", customerId, merchantId, amount);
                    return Mono.empty();
                }));
    }
}
