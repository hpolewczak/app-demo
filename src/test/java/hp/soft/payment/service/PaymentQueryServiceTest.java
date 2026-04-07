package hp.soft.payment.service;

import hp.soft.ledger.repository.TransactionRepository;
import hp.soft.payment.dto.PaymentDetail;
import hp.soft.payment.dto.Payment;
import hp.soft.payment.dto.PaymentStatus;
import hp.soft.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentQueryServiceTest {
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final PaymentQueryService paymentQueryService = new PaymentQueryService(paymentRepository, transactionRepository);

    private final BigDecimal amount = new BigDecimal("100.00");

    @Test
    void getPaymentDetail_returnsPaymentWithTransactions() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId).customerId(UUID.randomUUID()).merchantId(UUID.randomUUID())
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

        StepVerifier.create(paymentQueryService.getPaymentDetail(paymentId))
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

        StepVerifier.create(paymentQueryService.getPaymentDetail(paymentId))
                .expectErrorMatches(e -> e instanceof IllegalArgumentException
                        && e.getMessage().contains("Payment not found"))
                .verify();
    }
}
