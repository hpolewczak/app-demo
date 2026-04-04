package hp.soft.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PayOffRequest(
        @NotNull UUID paymentId,
        @NotNull String idempotencyKey
) {
}
