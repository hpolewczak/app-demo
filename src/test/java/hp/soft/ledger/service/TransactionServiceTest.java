package hp.soft.ledger.service;

import hp.soft.account.dto.Account;
import hp.soft.account.dto.AccountCode;
import hp.soft.account.dto.AccountType;
import hp.soft.ledger.dto.Transaction;
import hp.soft.ledger.dto.TxContext;
import hp.soft.ledger.repository.LedgerLineRepository;
import hp.soft.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class TransactionServiceTest {
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final LedgerLineRepository ledgerLineRepository = mock(LedgerLineRepository.class);
    private final TransactionService transactionService = new TransactionService(transactionRepository, ledgerLineRepository);

    private final UUID paymentId = UUID.randomUUID();
    private final UUID txId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("100.00");

    private final Account debitAccount = Account.builder()
            .id(UUID.randomUUID())
            .code(AccountCode.CUSTOMER_RECEIVABLE)
            .name("Customer Receivable")
            .type(AccountType.ASSET)
            .build();

    private final Account creditAccount = Account.builder()
            .id(UUID.randomUUID())
            .code(AccountCode.MERCHANT_PAYABLE)
            .name("Merchant Payable")
            .type(AccountType.LIABILITY)
            .build();

    @Test
    void createTransaction_insertsTransactionAndTwoLedgerLines() {
        Transaction tx = Transaction.builder().id(txId).paymentId(paymentId).description("Test").amount(amount).build();
        when(transactionRepository.insert(paymentId, "Test", amount)).thenReturn(Mono.just(tx));
        when(ledgerLineRepository.insert(any(), any(), any(), any())).thenReturn(Mono.empty());

        TxContext ctx = TxContext.builder()
                .paymentId(paymentId)
                .description("Test")
                .amount(amount)
                .debitAccount(debitAccount)
                .creditAccount(creditAccount)
                .build();

        StepVerifier.create(transactionService.createTransaction(ctx))
                .verifyComplete();

        verify(transactionRepository).insert(paymentId, "Test", amount);
        verify(ledgerLineRepository, times(2)).insert(any(), any(), any(), any());
    }

    @Test
    void createTransaction_passesCorrectAmountsToLedgerLines() {
        Transaction tx = Transaction.builder().id(txId).paymentId(paymentId).description("Test").amount(amount).build();
        when(transactionRepository.insert(paymentId, "Test", amount)).thenReturn(Mono.just(tx));
        when(ledgerLineRepository.insert(any(), any(), any(), any())).thenReturn(Mono.empty());

        TxContext ctx = TxContext.builder()
                .paymentId(paymentId)
                .description("Test")
                .amount(amount)
                .debitAccount(debitAccount)
                .creditAccount(creditAccount)
                .build();

        StepVerifier.create(transactionService.createTransaction(ctx))
                .verifyComplete();

        // debit line: amount in debit, null in credit
        verify(ledgerLineRepository).insert(eq(txId), eq(debitAccount.id()), eq(amount), isNull());
        // credit line: null in debit, amount in credit
        verify(ledgerLineRepository).insert(eq(txId), eq(creditAccount.id()), isNull(), eq(amount));
    }
}
