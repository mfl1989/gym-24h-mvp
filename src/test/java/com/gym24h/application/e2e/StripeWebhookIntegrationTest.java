package com.gym24h.application.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class StripeWebhookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from processed_webhook_events");
        jdbcTemplate.update("delete from audit_logs");
        jdbcTemplate.update("delete from invoices");
        jdbcTemplate.update("delete from subscriptions");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldActivateSubscriptionAndPersistProcessedWebhookEvent() throws Exception {
        Fixture fixture = createArrearsSubscriptionFixture();
        String eventId = "evt_happy_001";
        String payload = checkoutCompletedPayload(eventId, fixture.userId(), fixture.subscriptionId());

        mockMvc.perform(post("/subscriptions/webhooks")
                        .header("Stripe-Signature", createSignatureHeader(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        awaitUntil(() -> "ACTIVE".equals(currentSubscriptionStatus(fixture.subscriptionId())));
        awaitUntil(() -> processedEventCount(eventId) == 1);

        assertThat(currentSubscriptionStatus(fixture.subscriptionId())).isEqualTo("ACTIVE");
        assertThat(processedEventCount(eventId)).isEqualTo(1);
        assertThat(currentStripeCustomerId(fixture.subscriptionId())).isEqualTo("cus_test_" + fixture.userId());
        assertThat(currentStripeSubscriptionId(fixture.subscriptionId())).isEqualTo("sub_test_" + fixture.subscriptionId());
    }

    @Test
    void shouldIgnoreDuplicateWebhookEventWithSameEventId() throws Exception {
        Fixture fixture = createArrearsSubscriptionFixture();
        String eventId = "evt_duplicate_001";
        String payload = checkoutCompletedPayload(eventId, fixture.userId(), fixture.subscriptionId());
        String signatureHeader = createSignatureHeader(payload);

        mockMvc.perform(post("/subscriptions/webhooks")
                        .header("Stripe-Signature", signatureHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        awaitUntil(() -> "ACTIVE".equals(currentSubscriptionStatus(fixture.subscriptionId())));
        awaitUntil(() -> processedEventCount(eventId) == 1);

        mockMvc.perform(post("/subscriptions/webhooks")
                        .header("Stripe-Signature", signatureHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        Thread.sleep(200L);

        assertThat(currentSubscriptionStatus(fixture.subscriptionId())).isEqualTo("ACTIVE");
        assertThat(processedEventCount(eventId)).isEqualTo(1);
        assertThat(currentStripeCustomerId(fixture.subscriptionId())).isEqualTo("cus_test_" + fixture.userId());
        assertThat(currentStripeSubscriptionId(fixture.subscriptionId())).isEqualTo("sub_test_" + fixture.subscriptionId());
    }

    @Test
    void shouldRejectForgedStripeSignature() throws Exception {
        Fixture fixture = createArrearsSubscriptionFixture();
        String eventId = "evt_bad_sig_001";
        String payload = checkoutCompletedPayload(eventId, fixture.userId(), fixture.subscriptionId());

        mockMvc.perform(post("/subscriptions/webhooks")
                        .header("Stripe-Signature", "t=1710000000,v1=forged")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(currentSubscriptionStatus(fixture.subscriptionId())).isEqualTo("ARREARS");
        assertThat(processedEventCount(eventId)).isEqualTo(0);
    }

    @Test
    void shouldMoveSubscriptionToArrearsWhenInvoicePaymentFails() throws Exception {
        Fixture fixture = createActiveSubscriptionFixture();
        String eventId = "evt_invoice_failed_001";
        String payload = invoicePaymentFailedPayload(eventId, fixture.userId(), fixture.subscriptionId(), "card_declined");

        mockMvc.perform(post("/subscriptions/webhooks")
                        .header("Stripe-Signature", createSignatureHeader(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        awaitUntil(() -> "ARREARS".equals(currentSubscriptionStatus(fixture.subscriptionId())));
        awaitUntil(() -> processedEventCount(eventId) == 1);
        awaitUntil(() -> invoiceCountByStatus("FAILED") == 1);

        assertThat(currentSubscriptionStatus(fixture.subscriptionId())).isEqualTo("ARREARS");
        assertThat(processedEventCount(eventId)).isEqualTo(1);
        assertThat(paymentFailureAuditCount(fixture.userId())).isEqualTo(1);
        assertThat(invoiceCountByStatus("FAILED")).isEqualTo(1);
    }

    @Test
    void shouldRecoverAndExtendValidityWhenInvoicePaymentSucceeds() throws Exception {
        Fixture fixture = createArrearsSubscriptionFixture();
        Instant beforeEnd = currentPeriodEndAt(fixture.subscriptionId());
        String eventId = "evt_invoice_succeeded_001";
        String payload = invoicePaymentSucceededPayload(eventId, fixture.userId(), fixture.subscriptionId());

        mockMvc.perform(post("/subscriptions/webhooks")
                        .header("Stripe-Signature", createSignatureHeader(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        awaitUntil(() -> "ACTIVE".equals(currentSubscriptionStatus(fixture.subscriptionId())));
        awaitUntil(() -> processedEventCount(eventId) == 1);
        awaitUntil(() -> invoiceCountByStatus("PAID") == 1);

        assertThat(currentSubscriptionStatus(fixture.subscriptionId())).isEqualTo("ACTIVE");
        assertThat(currentPeriodEndAt(fixture.subscriptionId())).isEqualTo(beforeEnd.plusSeconds(30L * 24L * 60L * 60L));
        assertThat(invoiceCountByStatus("PAID")).isEqualTo(1);
    }

    private Fixture createArrearsSubscriptionFixture() {
        return createSubscriptionFixture("ARREARS");
    }

    private Fixture createActiveSubscriptionFixture() {
        return createSubscriptionFixture("ACTIVE");
    }

    private Fixture createSubscriptionFixture(String status) {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Instant now = Instant.now();

        jdbcTemplate.update(
                """
                insert into users (id, line_user_id, phone_number, display_name, membership_status, created_at, updated_at, version, is_deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                "line-" + userId,
                null,
                "Stripe Test User",
                "ACTIVE",
                now,
                now,
                0,
                false
        );

        jdbcTemplate.update(
                """
                insert into subscriptions (
                    id, user_id, plan_code, billing_cycle, status,
                    stripe_customer_id, stripe_subscription_id,
                    started_at, current_period_start_at, current_period_end_at,
                    canceled_at, cancellation_requested_at,
                    created_at, updated_at, version, is_deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                subscriptionId,
                userId,
                "STANDARD",
                "THIRTY_DAYS",
                status,
                "cus_test_" + userId,
                "sub_test_" + subscriptionId,
                now.minusSeconds(3600),
                now.minusSeconds(3600),
                now.plusSeconds(3600),
                null,
                null,
                now.minusSeconds(3600),
                now.minusSeconds(3600),
                0,
                false
        );

        return new Fixture(userId, subscriptionId);
    }

    private String checkoutCompletedPayload(String eventId, UUID userId, UUID subscriptionId) {
        return """
                {
                                    "object": "event",
                  "id": "%s",
                                    "api_version": "2024-06-20",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                                            "object": "checkout.session",
                      "id": "cs_test_%s",
                      "customer": "cus_test_%s",
                      "subscription": "sub_test_%s",
                      "metadata": {
                        "userId": "%s",
                        "subscriptionId": "%s"
                      }
                    }
                  }
                }
                """.formatted(eventId, subscriptionId, userId, subscriptionId, userId, subscriptionId);
    }

        private String invoicePaymentFailedPayload(String eventId, UUID userId, UUID subscriptionId, String failureReason) {
                return invoicePayload(eventId, userId, subscriptionId, "invoice.payment_failed", failureReason);
        }

        private String invoicePaymentSucceededPayload(String eventId, UUID userId, UUID subscriptionId) {
                return invoicePayload(eventId, userId, subscriptionId, "invoice.payment_succeeded", null);
        }

        private String invoicePayload(String eventId, UUID userId, UUID subscriptionId, String type, String failureReason) {
                String failureReasonField = failureReason == null ? "" : "\n            \"failureReason\": \"%s\",".formatted(failureReason);
                int amountPaid = "invoice.payment_succeeded".equals(type) ? 3000 : 0;
                return """
                                {
                                    "object": "event",
                                    "id": "%s",
                                    "api_version": "2024-06-20",
                                    "type": "%s",
                                    "data": {
                                        "object": {
                                            "object": "invoice",
                                            "id": "in_test_%s",
                                            "created": 1710000000,
                                            "customer": "cus_test_%s",
                                            "subscription": "sub_test_%s",
                                            "amount_paid": %d,
                                            "currency": "usd",
                                            "hosted_invoice_url": "https://stripe.test/invoices/%s",
                                            "due_date": 1710003600,
                                            "metadata": {
                                                "userId": "%s",%s
                                                "subscriptionId": "%s"
                                            }
                                        }
                                    }
                                }
                                """.formatted(eventId, type, subscriptionId, userId, subscriptionId, amountPaid, subscriptionId, userId, failureReasonField, subscriptionId);
        }

    private String createSignatureHeader(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String signedPayload = timestamp + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        return "t=%d,v1=%s".formatted(timestamp, signature);
    }

    private String currentSubscriptionStatus(UUID subscriptionId) {
        return jdbcTemplate.queryForObject(
                "select status from subscriptions where id = ?",
                String.class,
                subscriptionId
        );
    }

    private int processedEventCount(String eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from processed_webhook_events where event_id = ?",
                Integer.class,
                eventId
        );
        return count == null ? 0 : count;
    }

    private String currentStripeCustomerId(UUID subscriptionId) {
        return jdbcTemplate.queryForObject(
                "select stripe_customer_id from subscriptions where id = ?",
                String.class,
                subscriptionId
        );
    }

    private String currentStripeSubscriptionId(UUID subscriptionId) {
        return jdbcTemplate.queryForObject(
                "select stripe_subscription_id from subscriptions where id = ?",
                String.class,
                subscriptionId
        );
    }

    private Instant currentPeriodEndAt(UUID subscriptionId) {
        return jdbcTemplate.queryForObject(
                "select current_period_end_at from subscriptions where id = ?",
                Instant.class,
                subscriptionId
        );
    }

    private int paymentFailureAuditCount(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where user_id = ? and action = ? and result = ?",
                Integer.class,
                userId,
                "SUBSCRIPTION_PAYMENT",
                "FAILURE"
        );
        return count == null ? 0 : count;
    }

    private int invoiceCountByStatus(String status) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from invoices where status = ?",
                Integer.class,
                status
        );
        return count == null ? 0 : count;
    }

    private void awaitUntil(Condition condition) throws Exception {
        long deadline = System.currentTimeMillis() + 3_000L;
        while (System.currentTimeMillis() < deadline) {
            if (condition.matches()) {
                return;
            }
            Thread.sleep(50L);
        }
        assertThat(condition.matches()).isTrue();
    }

    @FunctionalInterface
    private interface Condition {
        boolean matches() throws Exception;
    }

    private record Fixture(UUID userId, UUID subscriptionId) {
    }
}