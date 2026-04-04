package hp.soft.payment.service;

import hp.soft.account.service.AccountService;
import hp.soft.ledger.repository.TransactionRepository;
import hp.soft.ledger.service.LedgerService;
import hp.soft.payment.dto.*;
import hp.soft.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Mono<Payment> purchase(PurchaseRequest request) {
        return paymentRepository.insert(request.customerId(), request.merchantId(), request.amount())
                .flatMap(payment ->
                        Mono.zip(
                                accountService.findOrCreateCustomerReceivable(request.customerId()),
                                accountService.findOrCreateMerchantPayable(request.merchantId()),
                                accountService.findZilchCash()
                        ).flatMap(accounts ->
                                ledgerService.recordPurchase(
                                        payment.id(),
                                        payment.amount(),
                                        accounts.getT1(),
                                        accounts.getT2(),
                                        accounts.getT3()
                                ).thenReturn(payment)
                        )
                );
    }

    @Transactional
    public Mono<Payment> payOff(PayOffRequest request) {
        return paymentRepository.findById(request.paymentId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Payment not found: " + request.paymentId())))
                .flatMap(payment -> {
                    if (payment.status() != PaymentStatus.CREATED) {
                        return Mono.error(new IllegalStateException("Payment is not in CREATED status: " + payment.status()));
                    }
                    return Mono.zip(
                            accountService.findOrCreateCustomerReceivable(payment.customerId()),
                            accountService.findZilchCash()
                    ).flatMap(accounts ->
                            ledgerService.recordRepayment(
                                    payment.id(),
                                    payment.amount(),
                                    accounts.getT1(),
                                    accounts.getT2()
                            ).then(paymentRepository.updateStatus(payment.id(), PaymentStatus.PAID))
                    );
                });
    }

    @Transactional(readOnly = true)
    public Mono<PaymentDetail> getPaymentDetail(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Payment not found: " + paymentId)))
                .flatMap(payment ->
                        transactionRepository.findDetailsByPaymentId(paymentId)
                                .map(transactions -> PaymentDetail.builder()
                                        .id(payment.id())
                                        .customerId(payment.customerId())
                                        .merchantId(payment.merchantId())
                                        .amount(payment.amount())
                                        .status(payment.status())
                                        .createdAt(payment.createdAt())
                                        .transactions(transactions)
                                        .build())
                );
    }
}
