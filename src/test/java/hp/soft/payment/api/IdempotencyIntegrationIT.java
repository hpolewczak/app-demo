package hp.soft.payment.api;

import hp.soft.BaseIntegrationTestIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

class IdempotencyIntegrationIT extends BaseIntegrationTestIT {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void duplicateIdempotencyKey_returns409() {
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        String idempotencyKey = "idem-" + UUID.randomUUID();

        // Create first payment and pay off
        var payment1 = webTestClient.post().uri("/api/v1/payments/purchase")
                .bodyValue(new PurchaseBody(customerId, merchantId, new BigDecimal("100.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .returnResult().getResponseBody();

        webTestClient.post().uri("/api/v1/payments/pay-off")
                .bodyValue(new PayOffBody(payment1.id(), idempotencyKey))
                .exchange()
                .expectStatus().isOk();

        // Create second payment and try to pay off with the same idempotency key
        var payment2 = webTestClient.post().uri("/api/v1/payments/purchase")
                .bodyValue(new PurchaseBody(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("200.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .returnResult().getResponseBody();

        webTestClient.post().uri("/api/v1/payments/pay-off")
                .bodyValue(new PayOffBody(payment2.id(), idempotencyKey))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void concurrentPayOffs_onlyOneSucceeds() {
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        var payment = webTestClient.post().uri("/api/v1/payments/purchase")
                .bodyValue(new PurchaseBody(customerId, merchantId, new BigDecimal("150.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .returnResult().getResponseBody();

        // Fire 5 concurrent pay-off requests with different idempotency keys
        var results = Flux.range(1, 5)
                .flatMap(i -> Mono.fromCallable(() ->
                        webTestClient.post().uri("/api/v1/payments/pay-off")
                                .bodyValue(new PayOffBody(payment.id(), "key-" + i))
                                .exchange()
                                .expectBody()
                                .returnResult()
                                .getStatus()
                                .value()
                ))
                .collectList()
                .block();

        long successes = results.stream().filter(status -> status == 200).count();
        long conflicts = results.stream().filter(status -> status == 409).count();

        // Exactly one should succeed, rest should be 409
        assert successes == 1 : "Expected exactly 1 success, got " + successes;
        assert conflicts == 4 : "Expected 4 conflicts, got " + conflicts;
    }

    record PurchaseBody(UUID customerId, UUID merchantId, BigDecimal amount) {}
    record PayOffBody(UUID paymentId, String idempotencyKey) {}
    record PaymentResponse(UUID id) {}
}
