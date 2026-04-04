package hp.soft.ledger.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record LedgerLine(
        UUID id,
        UUID transactionId,
        UUID accountId,
        BigDecimal debit,
        BigDecimal credit,
        OffsetDateTime createdAt
) {
}
