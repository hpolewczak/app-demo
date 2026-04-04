package hp.soft.payment.service;

import hp.soft.account.dto.Account;
import hp.soft.account.dto.AccountCode;
import hp.soft.account.dto.AccountType;
import hp.soft.account.service.AccountService;
import hp.soft.ledger.repository.TransactionRepository;
import hp.soft.ledger.service.LedgerService;
import hp.soft.payment.dto.*;
import hp.soft.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final AccountService accountService = mock(AccountService.class);
    private final LedgerService ledgerService = mock(LedgerService.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final PaymentService paymentService = new PaymentService(paymentRepository, accountService, ledgerService, transactionRepository);

    private final UUID customerId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();
    private final BigDecimal amount = new BigDecimal("100.00");

    private final Account customerReceivable = Account.builder()
            .id(UUID.randomUUID()).code(AccountCode.CUSTOMER_RECEIVABLE).name("Customer Receivable")
            .type(AccountType.ASSET).customerId(customerId).build();
    private final Account merchantPayable = Account.builder()
            .id(UUID.randomUUID()).code(AccountCode.MERCHANT_PAYABLE).name("Merchant Payable")
            .type(AccountType.LIABILITY).merchantId(merchantId).build();
    private final Account zilchCash = Account.builder()
            .id(UUID.randomUUID()).code(AccountCode.ZILCH_CASH).name("Zilch Cash")
            .type(AccountType.ASSET).build();

    @Test
    void purchase_createsPaymentAndLedgerEntries() {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID()).customerId(customerId).merchantId(merchantId)
                .amount(amount).status(PaymentStatus.CREATED).createdAt(OffsetDateTime.now()).build();

        when(paymentRepository.insert(customerId, merchantId, amount)).thenReturn(Mono.just(payment));
        when(accountService.findOrCreateCustomerReceivable(customerId)).thenReturn(Mono.just(customerReceivable));
        when(accountService.findOrCreateMerchantPayable(merchantId)).thenReturn(Mono.just(merchantPayable));
        when(accountService.findZilchCash()).thenReturn(Mono.just(zilchCash));
        when(ledgerService.recordPurchase(eq(payment.id()), eq(amount), any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.purchase(new PurchaseRequest(customerId, merchantId, amount)))
                .expectNextMatches(p -> p.id().equals(payment.id()) && p.status() == PaymentStatus.CREATED)
                .verifyComplete();

        verify(ledgerService).recordPurchase(payment.id(), amount, customerReceivable, merchantPayable, zilchCash);
    }

    @Test
    void payOff_updatesStatusToPaid() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId).customerId(customerId).merchantId(merchantId)
                .amount(amount).status(PaymentStatus.CREATED).createdAt(OffsetDateTime.now()).build();
        Payment paidPayment = Payment.builder()
                .id(paymentId).customerId(customerId).merchantId(merchantId)
                .amount(amount).status(PaymentStatus.PAID).createdAt(OffsetDateTime.now()).build();

        when(paymentRepository.findById(paymentId)).thenReturn(Mono.just(payment));
        when(accountService.findOrCreateCustomerReceivable(customerId)).thenReturn(Mono.just(customerReceivable));
        when(accountService.findZilchCash()).thenReturn(Mono.just(zilchCash));
        when(ledgerService.recordRepayment(eq(paymentId), eq(amount), any(), any())).thenReturn(Mono.empty());
        when(paymentRepository.updateStatus(paymentId, PaymentStatus.PAID)).thenReturn(Mono.just(paidPayment));

        StepVerifier.create(paymentService.payOff(new PayOffRequest(paymentId, "key-1")))
                .expectNextMatches(p -> p.status() == PaymentStatus.PAID)
                .verifyComplete();

        verify(ledgerService).recordRepayment(paymentId, amount, customerReceivable, zilchCash);
    }

    @Test
    void payOff_failsWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.payOff(new PayOffRequest(paymentId, "key-1")))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("Payment not found"))
                .verify();
    }

    @Test
    void payOff_failsWhenPaymentAlreadyPaid() {
        UUID paymentId = UUID.randomUUID();
        Payment paidPayment = Payment.builder()
                .id(paymentId).customerId(customerId).merchantId(merchantId)
                .amount(amount).status(PaymentStatus.PAID).createdAt(OffsetDateTime.now()).build();

        when(paymentRepository.findById(paymentId)).thenReturn(Mono.just(paidPayment));

        StepVerifier.create(paymentService.payOff(new PayOffRequest(paymentId, "key-1")))
                .expectErrorMatches(e -> e instanceof IllegalStateException
                        && e.getMessage().contains("not in CREATED status"))
                .verify();
    }

    @Test
    void getPaymentDetail_returnsPaymentWithTransactions() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId).customerId(customerId).merchantId(merchantId)
                .amount(amount).status(PaymentStatus.CREATED).createdAt(OffsetDateTime.now()).build();

        List<PaymentDetail.TransactionDetail> txDetails = List.of(
                PaymentDetail.TransactionDetail.builder()
                        .id(UUID.randomUUID()).description("Purchase obligation").amount(amount)
                        .createdAt(OffsetDateTime.now())
                        .lines(List.of(
                                PaymentDetail.LedgerLineDetail.builder()
                                        .accountCode("CUSTOMER_RECEIVABLE").accountName("Customer Receivable")
                                        .debit(amount).credit(null).build(),
                                PaymentDetail.LedgerLineDetail.builder()
                                        .accountCode("MERCHANT_PAYABLE").accountName("Merchant Payable")
                                        .debit(null).credit(amount).build()
                        )).build()
        );

        when(paymentRepository.findById(paymentId)).thenReturn(Mono.just(payment));
        when(transactionRepository.findDetailsByPaymentId(paymentId)).thenReturn(Mono.just(txDetails));

        StepVerifier.create(paymentService.getPaymentDetail(paymentId))
                .assertNext(detail -> {
                    assertEquals(paymentId, detail.id());
                    assertEquals(1, detail.transactions().size());
                    assertEquals(2, detail.transactions().getFirst().lines().size());
                })
                .verifyComplete();
    }

    @Test
    void getPaymentDetail_failsWhenPaymentNotFound() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Mono.empty());

        StepVerifier.create(paymentService.getPaymentDetail(paymentId))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("Payment not found"))
                .verify();
    }
}
