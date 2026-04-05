package hp.soft.payment.api;

import hp.soft.BaseIntegrationTestIT;
import hp.soft.payment.dto.Payment;
import hp.soft.payment.dto.PaymentDetail;
import hp.soft.payment.dto.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentIntegrationIT extends BaseIntegrationTestIT {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    void fullPurchaseAndPayOffFlow() {
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        // 1. Purchase
        Payment purchase = webTestClient.post().uri("/api/v1/payments/purchase")
                .bodyValue(new PurchaseBody(customerId, merchantId, new BigDecimal("250.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Payment.class)
                .returnResult().getResponseBody();

        assertNotNull(purchase);
        assertEquals(PaymentStatus.CREATED, purchase.status());
        assertEquals(0, purchase.amount().compareTo(new BigDecimal("250.0000")));

        // 2. Get payment detail — should have 2 transactions, 4 ledger lines
        PaymentDetail detail = webTestClient.get().uri("/api/v1/payments/{id}", purchase.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDetail.class)
                .returnResult().getResponseBody();

        assertNotNull(detail);
        assertEquals(2, detail.transactions().size());
        assertEquals(2, detail.transactions().get(0).lines().size());
        assertEquals(2, detail.transactions().get(1).lines().size());

        // 3. Pay off
        Payment payOff = webTestClient.post().uri("/api/v1/payments/pay-off")
                .bodyValue(new PayOffBody(purchase.id(), "key-" + UUID.randomUUID()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Payment.class)
                .returnResult().getResponseBody();

        assertNotNull(payOff);
        assertEquals(PaymentStatus.PAID, payOff.status());

        // 4. Get payment detail after pay-off — should have 3 transactions, 6 ledger lines
        PaymentDetail detailAfterPayOff = webTestClient.get().uri("/api/v1/payments/{id}", purchase.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentDetail.class)
                .returnResult().getResponseBody();

        assertNotNull(detailAfterPayOff);
        assertEquals(3, detailAfterPayOff.transactions().size());
        assertEquals(PaymentStatus.PAID, detailAfterPayOff.status());
    }

    @Test
    void payOff_returnsConflictWhenAlreadyPaid() {
        UUID customerId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        Payment purchase = webTestClient.post().uri("/api/v1/payments/purchase")
                .bodyValue(new PurchaseBody(customerId, merchantId, new BigDecimal("100.00")))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Payment.class)
                .returnResult().getResponseBody();

        // First pay-off succeeds
        webTestClient.post().uri("/api/v1/payments/pay-off")
                .bodyValue(new PayOffBody(purchase.id(), "key-" + UUID.randomUUID()))
                .exchange()
                .expectStatus().isOk();

        // Second pay-off returns 409
        webTestClient.post().uri("/api/v1/payments/pay-off")
                .bodyValue(new PayOffBody(purchase.id(), "key-" + UUID.randomUUID()))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void payOff_returnsBadRequestWhenPaymentNotFound() {
        webTestClient.post().uri("/api/v1/payments/pay-off")
                .bodyValue(new PayOffBody(UUID.randomUUID(), "key-1"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getPaymentDetail_returnsBadRequestWhenNotFound() {
        webTestClient.get().uri("/api/v1/payments/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isBadRequest();
    }

    record PurchaseBody(UUID customerId, UUID merchantId, BigDecimal amount) {}
    record PayOffBody(UUID paymentId, String idempotencyKey) {}
}
