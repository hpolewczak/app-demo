package hp.soft.report.dto;

import hp.soft.ledger.dto.AccountBalance;
import lombok.Builder;

import java.util.List;

@Builder
public record TrialBalance(
        List<AccountBalance> accounts,
        boolean balanced
) {
}
