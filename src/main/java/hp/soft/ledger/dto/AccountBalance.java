package hp.soft.ledger.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AccountBalance(
        String code,
        String name,
        String type,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal balance
) {
}
