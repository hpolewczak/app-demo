package hp.soft.ledger.service;

import hp.soft.account.dto.Account;
import hp.soft.ledger.dto.TxContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final TransactionService transactionService;

    public Mono<Void> recordPurchase(UUID paymentId, BigDecimal amount,
                                     Account customerReceivable,
                                     Account merchantPayable,
                                     Account zilchCash) {
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
        return transactionService.createTransaction(TxContext.builder()
                .paymentId(paymentId)
                .description("Customer repayment")
                .amount(amount)
                .debitAccount(zilchCash)
                .creditAccount(customerReceivable)
                .build());
    }

}
