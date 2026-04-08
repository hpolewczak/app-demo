package hp.soft.ledger.service;

import hp.soft.account.dto.Account;
import hp.soft.ledger.dto.TxContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {
    private final TransactionService transactionService;

    public Mono<Void> recordPurchase(UUID paymentId, BigDecimal amount,
                                     Account customerReceivable,
                                     Account merchantPayable,
                                     Account zilchCash) {
        log.debug("Recording purchase ledger entries: paymentId={}, amount={}", paymentId, amount);
        var purchaseObligation = transactionService.createTransaction(TxContext.builder()
                .paymentId(paymentId)
                .description("Purchase obligation")
                .amount(amount)
                .debitAccount(customerReceivable)
                .creditAccount(merchantPayable)
                .build());

        var merchantSettlement = transactionService.createTransaction(TxContext.builder()
                .paymentId(paymentId)
                .description("Merchant settlement")
                .amount(amount)
                .debitAccount(merchantPayable)
                .creditAccount(zilchCash)
                .build());

        return purchaseObligation.then(merchantSettlement);
    }

    public Mono<Void> recordRepayment(UUID paymentId, BigDecimal amount,
                                      Account customerReceivable,
                                      Account zilchCash) {
        log.debug("Recording repayment ledger entry: paymentId={}, amount={}", paymentId, amount);
        return transactionService.createTransaction(TxContext.builder()
                .paymentId(paymentId)
                .description("Customer repayment")
                .amount(amount)
                .debitAccount(zilchCash)
                .creditAccount(customerReceivable)
                .build());
    }

}
