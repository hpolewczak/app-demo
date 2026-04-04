package hp.soft.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseRequest(
        @NotNull UUID customerId,
        @NotNull UUID merchantId,
        @NotNull @Positive BigDecimal amount
) {
}
