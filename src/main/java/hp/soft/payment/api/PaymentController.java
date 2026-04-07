package hp.soft.payment.api;

import hp.soft.payment.dto.*;
import hp.soft.payment.service.PaymentQueryService;
import hp.soft.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentQueryService paymentQueryService;

    @PostMapping("/purchase")
    public Mono<Payment> purchase(@Valid @RequestBody PurchaseRequest request) {
        return paymentService.purchase(request);
    }

    @PostMapping("/pay-off")
    public Mono<Payment> payOff(@Valid @RequestBody PayOffRequest request) {
        return paymentService.payOff(request);
    }

    @GetMapping("/{paymentId}")
    public Mono<PaymentDetail> getPaymentDetail(@PathVariable UUID paymentId) {
        return paymentQueryService.getPaymentDetail(paymentId);
    }
}
