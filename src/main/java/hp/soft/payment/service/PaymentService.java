package hp.soft.payment.service;

import hp.soft.account.service.AccountService;
import hp.soft.ledger.service.LedgerService;
import hp.soft.payment.dto.*;
import hp.soft.payment.repository.PaymentRepository;
import hp.soft.payment.service.validation.PurchaseValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final PurchaseValidator purchaseValidator;
    private final TransactionalOperator txOperator;

    public Mono<Payment> purchase(PurchaseRequest request) {
        long start = System.nanoTime();
        log.info("Processing purchase: customer={}, merchant={}, amount={}",
                request.customerId(), request.merchantId(), request.amount());
        return purchaseValidator.validate(request.customerId(), request.merchantId(), request.amount())
                .then(Mono.defer(() -> executePurchase(request)))
                .doOnSuccess(p -> log.info("Purchase completed: paymentId={}, durationMs={}",
                        p.id(), Duration.ofNanos(System.nanoTime() - start).toMillis()))
                .doOnError(e -> log.error("Purchase failed: customer={}, merchant={}, amount={}, durationMs={} — {}",
                        request.customerId(), request.merchantId(), request.amount(),
                        Duration.ofNanos(System.nanoTime() - start).toMillis(), e.getMessage()));
    }

    private Mono<Payment> executePurchase(PurchaseRequest request) {
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
                )
                .as(txOperator::transactional);
    }

    public Mono<Payment> payOff(PayOffRequest request) {
        long start = System.nanoTime();
        log.info("Processing pay-off: paymentId={}, idempotencyKey={}", request.paymentId(), request.idempotencyKey());
        return paymentRepository.findByIdForUpdate(request.paymentId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Payment not found: " + request.paymentId())))
                .flatMap(this::ensureCreated)
                .flatMap(payment -> recordRepayment(payment, request.idempotencyKey()))
                .as(txOperator::transactional)
                .doOnSuccess(p -> log.info("Pay-off completed: paymentId={}, durationMs={}",
                        p.id(), Duration.ofNanos(System.nanoTime() - start).toMillis()))
                .doOnError(e -> log.error("Pay-off failed: paymentId={}, durationMs={} — {}",
                        request.paymentId(), Duration.ofNanos(System.nanoTime() - start).toMillis(), e.getMessage()));
    }

    private Mono<Payment> ensureCreated(Payment payment) {
        if (payment.status() != PaymentStatus.CREATED) {
            log.warn("Pay-off rejected: paymentId={}, currentStatus={}", payment.id(), payment.status());
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
