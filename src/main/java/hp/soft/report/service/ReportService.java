package hp.soft.report.service;

import hp.soft.ledger.dto.AccountBalance;
import hp.soft.report.dto.TrialBalance;
import hp.soft.ledger.repository.LedgerLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final LedgerLineRepository ledgerLineRepository;

    public Mono<TrialBalance> getTrialBalance() {
        return ledgerLineRepository.findAccountBalances()
                .collectList()
                .map(accounts -> TrialBalance.builder()
                        .accounts(accounts)
                        .balanced(isBalanced(accounts))
                        .build());
    }

    private boolean isBalanced(List<AccountBalance> accounts) {
        BigDecimal totalDebits =  sum(accounts, AccountBalance::totalDebits);
        BigDecimal totalCredits = sum(accounts, AccountBalance::totalCredits);
        return totalDebits.compareTo(totalCredits) == 0;
    }

    private <T> BigDecimal sum(List<T> accounts, Function<T, BigDecimal> mapper) {
        return accounts.stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
