package hp.soft.ledger.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record Transaction(
        UUID id,
        String description,
        UUID paymentId,
        BigDecimal amount,
        OffsetDateTime createdAt
) {
}
