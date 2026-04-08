package hp.soft.payment.service.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class CreditCheckService {
    private static final BigDecimal CREDIT_LIMIT = new BigDecimal("10000.00");

    public Mono<Void> check(UUID customerId, BigDecimal amount) {
        return Mono.delay(Duration.ofMillis(50))
                .then(Mono.defer(() -> {
                    if (amount.compareTo(CREDIT_LIMIT) > 0) {
                        log.warn("Credit check failed: amount {} exceeds limit {} for customer {}", amount, CREDIT_LIMIT, customerId);
                        return Mono.error(new IllegalArgumentException(
                                "Credit check failed: amount %s exceeds limit %s for customer %s"
                                        .formatted(amount, CREDIT_LIMIT, customerId)));
                    }
                    log.debug("Credit check passed for customer {} amount {}", customerId, amount);
                    return Mono.empty();
                }));
    }
}
