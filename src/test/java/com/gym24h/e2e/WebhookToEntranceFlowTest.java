package com.gym24h.e2e;

import com.gym24h.Gym24hApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = Gym24hApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebhookToEntranceFlowTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void shouldAcceptWebhookAndOpenDoorForActiveSubscription() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        jdbcTemplate.update(
                "insert into users (id, line_user_id, active, version, is_deleted, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?)",
                userId, "line-user-e2e", true, 0L, false, now, now
        );
        jdbcTemplate.update(
                "insert into subscriptions (id, user_id, billing_cycle, status, valid_from, valid_until, cancellation_requested_at, version, is_deleted, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                subscriptionId, userId, "THIRTY_DAYS", "ACTIVE", now, now.plusSeconds(3600), null, 0L, false, now, now
        );

        HttpHeaders webhookHeaders = new HttpHeaders();
        webhookHeaders.add("Stripe-Event-Id", "evt_1");
        webhookHeaders.add("Stripe-Event-Type", "invoice.paid");
        webhookHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> webhookResponse = testRestTemplate.exchange(
                "http://localhost:" + port + "/webhooks/stripe",
                HttpMethod.POST,
                new HttpEntity<>("{}", webhookHeaders),
                String.class
        );

        HttpHeaders entranceHeaders = new HttpHeaders();
        entranceHeaders.setContentType(MediaType.APPLICATION_JSON);
        String entranceBody = """
                {
                  \"userId\": \"%s\",
                  \"subscriptionId\": \"%s\",
                  \"requestId\": \"req-1\",
                  \"requestedAt\": \"%s\"
                }
                """.formatted(userId, subscriptionId, now.plusSeconds(60));

        ResponseEntity<String> entranceResponse = testRestTemplate.exchange(
                "http://localhost:" + port + "/entrances/open",
                HttpMethod.POST,
                new HttpEntity<>(entranceBody, entranceHeaders),
                String.class
        );

        assertEquals(200, webhookResponse.getStatusCode().value());
        assertEquals(200, entranceResponse.getStatusCode().value());
    }
}