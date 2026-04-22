package com.gym24h.application.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MeSubscriptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private com.gym24h.infrastructure.external.stripe.StripeClient stripeClient;

    @BeforeEach
    void setUp() throws Exception {
        jdbcTemplate.update("delete from processed_webhook_events");
        jdbcTemplate.update("delete from audit_logs");
        jdbcTemplate.update("delete from invoices");
        jdbcTemplate.update("delete from subscriptions");
        jdbcTemplate.update("delete from users");

        when(stripeClient.scheduleCancellationAtPeriodEnd(eq("sub_cancel_me_001")))
                .thenReturn(new com.stripe.model.Subscription());
        when(stripeClient.revokeCancellationAtPeriodEnd(eq("sub_cancel_me_001")))
            .thenReturn(new com.stripe.model.Subscription());
    }

    @Test
    void shouldReturnMySubscriptionDetails() throws Exception {
        Fixture fixture = createFixture(Instant.now().minusSeconds(5L * 24L * 60L * 60L), null);
        String authToken = login(fixture.userId());

        mockMvc.perform(get("/me/subscription")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.cancelAtPeriodEnd").value(false))
                .andExpect(jsonPath("$.data.withinCancellationWindow").value(true));
    }

    @Test
    void shouldReturnCancelFlagInMeSummary() throws Exception {
        Fixture fixture = createFixture(Instant.now().minusSeconds(5L * 24L * 60L * 60L), Instant.now().minusSeconds(60L));
        String authToken = login(fixture.userId());

        mockMvc.perform(get("/me")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancelAtPeriodEnd").value(true));
    }

    @Test
    void shouldCancelSubscriptionAtPeriodEndWithinWindow() throws Exception {
        Fixture fixture = createFixture(Instant.now().minusSeconds(5L * 24L * 60L * 60L), null);
        String authToken = login(fixture.userId());

        mockMvc.perform(post("/me/subscription/cancel")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("CANCELLATION_REQUEST_ACCEPTED"));

        verify(stripeClient).scheduleCancellationAtPeriodEnd("sub_cancel_me_001");
    }

    @Test
    void shouldRevokeCancellationAtPeriodEnd() throws Exception {
        Fixture fixture = createFixture(Instant.now().minusSeconds(20L * 24L * 60L * 60L), Instant.now().minusSeconds(60L));
        String authToken = login(fixture.userId());

        mockMvc.perform(post("/me/subscription/revoke-cancel")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("CANCELLATION_REVOCATION_ACCEPTED"));

        verify(stripeClient).revokeCancellationAtPeriodEnd("sub_cancel_me_001");

        mockMvc.perform(get("/me/subscription")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancelAtPeriodEnd").value(false));
    }

    private Fixture createFixture(Instant currentPeriodStartAt, Instant cancellationRequestedAt) {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant currentPeriodEndAt = currentPeriodStartAt.plusSeconds(30L * 24L * 60L * 60L);

        jdbcTemplate.update(
                """
                insert into users (id, line_user_id, phone_number, display_name, membership_status, created_at, updated_at, version, is_deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                "line-" + userId,
                "09012345678",
                "Subscription User",
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
                "cus_cancel_me_001",
                "sub_cancel_me_001",
                currentPeriodStartAt,
                currentPeriodStartAt,
                currentPeriodEndAt,
                cancellationRequestedAt == null ? null : currentPeriodEndAt,
                cancellationRequestedAt,
                now,
                now,
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