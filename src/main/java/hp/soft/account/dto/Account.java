package hp.soft.account.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record Account(
        UUID id,
        AccountCode code,
        String name,
        AccountType type,
        UUID customerId,
        UUID merchantId
) {
}
