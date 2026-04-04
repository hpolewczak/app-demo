package hp.soft.payment.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record Payment(
        UUID id,
        UUID customerId,
        UUID merchantId,
        BigDecimal amount,
        PaymentStatus status,
        String idempotencyKey,
        OffsetDateTime createdAt
) {
}
