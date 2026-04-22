package com.gym24h.application.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from processed_webhook_events");
        jdbcTemplate.update("delete from audit_logs");
        jdbcTemplate.update("delete from invoices");
        jdbcTemplate.update("delete from subscriptions");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldReturnCurrentUserProfile() throws Exception {
        Fixture fixture = createFixture();
        String authToken = login(fixture.userId());

        mockMvc.perform(get("/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.displayName").value("Frontend User"))
                .andExpect(jsonPath("$.data.membershipStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.subscriptionStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.subscriptionValidUntil").exists());
    }

    @Test
    void shouldReturnInvoiceHistoryInDescendingOrder() throws Exception {
        Fixture fixture = createFixture();
        String authToken = login(fixture.userId());

        mockMvc.perform(get("/me/invoices")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("PAID"))
                .andExpect(jsonPath("$.data[0].amount").value("30.00"))
                .andExpect(jsonPath("$.data[0].currency").value("USD"))
                .andExpect(jsonPath("$.data[1].status").value("FAILED"))
                .andExpect(jsonPath("$.data[1].amount").value("0.00"));
    }

    private Fixture createFixture() {
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
                "09012345678",
                "Frontend User",
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
                "ACTIVE",
                "cus_me_001",
                "sub_me_001",
                now.minusSeconds(86400),
                now.minusSeconds(86400),
                now.plusSeconds(86400),
                null,
                null,
                now.minusSeconds(86400),
                now.minusSeconds(86400),
                0,
                false
        );

        jdbcTemplate.update(
                """
                insert into invoices (
                    id, subscription_id, user_id, stripe_invoice_id, stripe_event_id,
                    amount, currency, status, invoice_url,
                    billed_at, paid_at, due_at,
                    created_at, updated_at, version, is_deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                subscriptionId,
                userId,
                "in_paid_001",
                "evt_paid_001",
                3000,
                "usd",
                "PAID",
                "https://stripe.test/invoices/paid",
                now.minusSeconds(3600),
                now.minusSeconds(3500),
                now.minusSeconds(3400),
                now.minusSeconds(3600),
                now.minusSeconds(3600),
                0,
                false
        );

        jdbcTemplate.update(
                """
                insert into invoices (
                    id, subscription_id, user_id, stripe_invoice_id, stripe_event_id,
                    amount, currency, status, invoice_url,
                    billed_at, paid_at, due_at,
                    created_at, updated_at, version, is_deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                subscriptionId,
                userId,
                "in_failed_001",
                "evt_failed_001",
                0,
                "usd",
                "FAILED",
                "https://stripe.test/invoices/failed",
                now.minusSeconds(7200),
                null,
                now.minusSeconds(7000),
                now.minusSeconds(7200),
                now.minusSeconds(7200),
                0,
                false
        );

        return new Fixture(userId);
    }

    private String login(UUID userId) throws Exception {
        String content = mockMvc.perform(post("/auth/debug/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "%s"
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(content);
        return root.path("data").path("authToken").asText();
    }

    private record Fixture(UUID userId) {
    }
}