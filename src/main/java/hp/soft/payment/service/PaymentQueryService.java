package hp.soft.payment.service;

import hp.soft.ledger.repository.TransactionRepository;
import hp.soft.payment.dto.PaymentDetail;
import hp.soft.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;

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
