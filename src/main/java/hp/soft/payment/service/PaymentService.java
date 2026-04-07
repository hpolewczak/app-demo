package hp.soft.payment.service;

import hp.soft.account.service.AccountService;
import hp.soft.ledger.service.LedgerService;
import hp.soft.payment.dto.*;
import hp.soft.payment.repository.PaymentRepository;
import hp.soft.payment.service.validation.PurchaseValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final PurchaseValidator purchaseValidator;

    @Transactional
    public Mono<Payment> purchase(PurchaseRequest request) {
        return purchaseValidator.validate(request.customerId(), request.merchantId(), request.amount())
                .then(Mono.defer(() -> paymentRepository.insert(request.customerId(), request.merchantId(), request.amount())))
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
        return paymentRepository.findByIdForUpdate(request.paymentId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Payment not found: " + request.paymentId())))
                .flatMap(this::ensureCreated)
                .flatMap(payment -> recordRepayment(payment, request.idempotencyKey()));
    }

    private Mono<Payment> ensureCreated(Payment payment) {
        if (payment.status() != PaymentStatus.CREATED) {
            return Mono.error(new IllegalStateException("Payment is not in CREATED status: " + payment.status()));
        }
        return Mono.just(payment);
    }

    private Mono<Payment> recordRepayment(Payment payment, String idempotencyKey) {
        return Mono.zip(
                        accountService.findOrCreateCustomerReceivable(payment.customerId()),
                        accountService.findZilchCash()
                )
                .flatMap(accounts -> ledgerService.recordRepayment(
                                payment.id(),
                                payment.amount(),
                                accounts.getT1(),
                                accounts.getT2()
                        ).then(paymentRepository.updateStatusWithIdempotencyKey(
                                payment.id(), PaymentStatus.PAID, idempotencyKey)));
    }
}
