package hp.soft.ledger.dto;

import hp.soft.account.dto.Account;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record TxContext(
        UUID paymentId,
        String description,
        BigDecimal amount,
        Account debitAccount,
        Account creditAccount
) {
}
