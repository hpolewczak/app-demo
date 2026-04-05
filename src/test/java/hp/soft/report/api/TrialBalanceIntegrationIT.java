package hp.soft.report.api;

import hp.soft.BaseIntegrationTestIT;
import hp.soft.report.dto.TrialBalance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrialBalanceIntegrationIT extends BaseIntegrationTestIT {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void trialBalance_isBalancedAfterPurchase() {
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        webTestClient.post().uri("/api/v1/payments/purchase")
                .bodyValue(new PurchaseBody(customerId, merchantId, new BigDecimal("500.00")))
                .exchange()
                .expectStatus().isOk();

        TrialBalance tb = webTestClient.get().uri("/api/v1/report/trial-balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(TrialBalance.class)
                .returnResult().getResponseBody();

        assertNotNull(tb);
        assertTrue(tb.balanced());
    }

    @Test
    void trialBalance_isBalancedAfterMultiplePurchases() {
        for (int i = 0; i < 3; i++) {
            webTestClient.post().uri("/api/v1/payments/purchase")
                    .bodyValue(new PurchaseBody(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00")))
                    .exchange()
                    .expectStatus().isOk();
        }

        TrialBalance tb = webTestClient.get().uri("/api/v1/report/trial-balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(TrialBalance.class)
                .returnResult().getResponseBody();

        assertNotNull(tb);
        assertTrue(tb.balanced());
    }

    @Test
    void trialBalance_isBalancedAfterPurchaseAndPayOff() {
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        var purchase = webTestClient.post().uri("/api/v1/payments/purchase")
                .bodyValue(new PurchaseBody(customerId, merchantId, new BigDecimal("300.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(PurchaseResponse.class)
                .returnResult().getResponseBody();

        webTestClient.post().uri("/api/v1/payments/pay-off")
                .bodyValue(new PayOffBody(purchase.id(), "key-" + UUID.randomUUID()))
                .exchange()
                .expectStatus().isOk();

        TrialBalance tb = webTestClient.get().uri("/api/v1/report/trial-balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody(TrialBalance.class)
                .returnResult().getResponseBody();

        assertNotNull(tb);
        assertTrue(tb.balanced());
    }

    record PurchaseBody(UUID customerId, UUID merchantId, BigDecimal amount) {}
    record PayOffBody(UUID paymentId, String idempotencyKey) {}
    record PurchaseResponse(UUID id) {}
}
