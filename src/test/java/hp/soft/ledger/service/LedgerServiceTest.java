package hp.soft.ledger.service;

import hp.soft.account.dto.Account;
import hp.soft.account.dto.AccountCode;
import hp.soft.account.dto.AccountType;
import hp.soft.ledger.dto.TxContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LedgerServiceTest {
    private final TransactionService transactionService = mock(TransactionService.class);
    private final LedgerService ledgerService = new LedgerService(transactionService);

    private final UUID paymentId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("250.00");

    private final Account customerReceivable = Account.builder()
            .id(UUID.randomUUID())
            .code(AccountCode.CUSTOMER_RECEIVABLE)
            .name("Customer Receivable")
            .type(AccountType.ASSET)
            .build();

    private final Account merchantPayable = Account.builder()
            .id(UUID.randomUUID())
            .code(AccountCode.MERCHANT_PAYABLE)
            .name("Merchant Payable")
            .type(AccountType.LIABILITY)
            .build();

    private final Account zilchCash = Account.builder()
            .id(UUID.randomUUID())
            .code(AccountCode.ZILCH_CASH)
            .name("Zilch Cash")
            .type(AccountType.ASSET)
            .build();

    @Test
    void recordPurchase_createsTwoTransactions() {
        when(transactionService.createTransaction(any())).thenReturn(Mono.empty());

        StepVerifier.create(ledgerService.recordPurchase(paymentId, amount, customerReceivable, merchantPayable, zilchCash))
                .verifyComplete();

        verify(transactionService, times(2)).createTransaction(any());
    }

    @Test
    void recordPurchase_usesCorrectAccountsForObligation() {
        ArgumentCaptor<TxContext> captor = ArgumentCaptor.forClass(TxContext.class);
        when(transactionService.createTransaction(any())).thenReturn(Mono.empty());

        StepVerifier.create(ledgerService.recordPurchase(paymentId, amount, customerReceivable, merchantPayable, zilchCash))
                .verifyComplete();

        verify(transactionService, times(2)).createTransaction(captor.capture());
        List<TxContext> contexts = captor.getAllValues();

        TxContext obligation = contexts.get(0);
        assertEquals(customerReceivable, obligation.debitAccount());
        assertEquals(merchantPayable, obligation.creditAccount());
        assertEquals(amount, obligation.amount());
    }

    @Test
    void recordPurchase_usesCorrectAccountsForSettlement() {
        ArgumentCaptor<TxContext> captor = ArgumentCaptor.forClass(TxContext.class);
        when(transactionService.createTransaction(any())).thenReturn(Mono.empty());

        StepVerifier.create(ledgerService.recordPurchase(paymentId, amount, customerReceivable, merchantPayable, zilchCash))
                .verifyComplete();

        verify(transactionService, times(2)).createTransaction(captor.capture());
        List<TxContext> contexts = captor.getAllValues();

        TxContext settlement = contexts.get(1);
        assertEquals(merchantPayable, settlement.debitAccount());
        assertEquals(zilchCash, settlement.creditAccount());
        assertEquals(amount, settlement.amount());
    }

    @Test
    void recordRepayment_createsOneTransaction() {
        ArgumentCaptor<TxContext> captor = ArgumentCaptor.forClass(TxContext.class);
        when(transactionService.createTransaction(any())).thenReturn(Mono.empty());

        StepVerifier.create(ledgerService.recordRepayment(paymentId, amount, customerReceivable, zilchCash))
                .verifyComplete();

        verify(transactionService, times(1)).createTransaction(captor.capture());

        // Repayment: debit zilchCash, credit customerReceivable
        TxContext repayment = captor.getValue();
        assertEquals(zilchCash, repayment.debitAccount());
        assertEquals(customerReceivable, repayment.creditAccount());
        assertEquals(amount, repayment.amount());
    }
}
