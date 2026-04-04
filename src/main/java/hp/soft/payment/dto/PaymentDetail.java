package hp.soft.payment.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record PaymentDetail(
        UUID id,
        UUID customerId,
        UUID merchantId,
        BigDecimal amount,
        PaymentStatus status,
        OffsetDateTime createdAt,
        List<TransactionDetail> transactions
) {

    @Builder
    public record TransactionDetail(
            UUID id,
            String description,
            BigDecimal amount,
            OffsetDateTime createdAt,
            List<LedgerLineDetail> lines
    ) {
    }

    @Builder
    public record LedgerLineDetail(
            String accountCode,
            String accountName,
            BigDecimal debit,
            BigDecimal credit
    ) {
    }
}
