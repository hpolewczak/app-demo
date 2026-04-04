package hp.soft.report.service;

import hp.soft.ledger.dto.AccountBalance;
import hp.soft.ledger.repository.LedgerLineRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {
    private final LedgerLineRepository ledgerLineRepository = mock(LedgerLineRepository.class);
    private final ReportService reportService = new ReportService(ledgerLineRepository);

    @Test
    void getTrialBalance_returnsBalancedWhenDebitsEqualCredits() {
        AccountBalance acc1 = AccountBalance.builder()
                .code("CUSTOMER_RECEIVABLE").name("Customer Receivable").type("ASSET")
                .totalDebits(new BigDecimal("500.00")).totalCredits(new BigDecimal("200.00"))
                .balance(new BigDecimal("300.00")).build();
        AccountBalance acc2 = AccountBalance.builder()
                .code("MERCHANT_PAYABLE").name("Merchant Payable").type("LIABILITY")
                .totalDebits(new BigDecimal("200.00")).totalCredits(new BigDecimal("500.00"))
                .balance(new BigDecimal("-300.00")).build();

        // total debits = 700, total credits = 700 -> balanced
        when(ledgerLineRepository.findAccountBalances()).thenReturn(Flux.just(acc1, acc2));

        StepVerifier.create(reportService.getTrialBalance())
                .assertNext(tb -> {
                    assertEquals(2, tb.accounts().size());
                    assertTrue(tb.balanced());
                })
                .verifyComplete();
    }

    @Test
    void getTrialBalance_returnsUnbalancedWhenDebitsDifferFromCredits() {
        AccountBalance acc1 = AccountBalance.builder()
                .code("CUSTOMER_RECEIVABLE").name("Customer Receivable").type("ASSET")
                .totalDebits(new BigDecimal("500.00")).totalCredits(new BigDecimal("100.00"))
                .balance(new BigDecimal("400.00")).build();
        AccountBalance acc2 = AccountBalance.builder()
                .code("MERCHANT_PAYABLE").name("Merchant Payable").type("LIABILITY")
                .totalDebits(new BigDecimal("200.00")).totalCredits(new BigDecimal("500.00"))
                .balance(new BigDecimal("-300.00")).build();

        // total debits = 700, total credits = 600 -> unbalanced
        when(ledgerLineRepository.findAccountBalances()).thenReturn(Flux.just(acc1, acc2));

        StepVerifier.create(reportService.getTrialBalance())
                .assertNext(tb -> {
                    assertEquals(2, tb.accounts().size());
                    assertFalse(tb.balanced());
                })
                .verifyComplete();
    }

    @Test
    void getTrialBalance_returnsEmptyListWhenNoLedgerLines() {
        when(ledgerLineRepository.findAccountBalances()).thenReturn(Flux.empty());

        StepVerifier.create(reportService.getTrialBalance())
                .assertNext(tb -> {
                    assertTrue(tb.accounts().isEmpty());
                    assertTrue(tb.balanced());
                })
                .verifyComplete();
    }
}
