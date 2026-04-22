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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class EntranceFlowIntegrationTest {

        @MockBean
        private com.gym24h.application.outbound.DoorLockClient doorLockClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
                doNothing().when(doorLockClient).unlock("MAIN_DOOR_01");
        jdbcTemplate.update("delete from audit_logs");
        jdbcTemplate.update("delete from invoices");
        jdbcTemplate.update("delete from subscriptions");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldCompleteHappyPathAndPersistSuccessAuditLog() throws Exception {
        Fixture fixture = createActiveSubscriptionFixture();

        String authToken = login(fixture.userId());
        String qrToken = issueQrToken(authToken, fixture.subscriptionId());

        mockMvc.perform(post("/entrances/open")
                        .header("Authorization", "Bearer " + authToken)
                        .header("X-Request-Id", "open-happy-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "qrToken": "%s"
                                }
                                """.formatted(qrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("OPEN_REQUEST_ACCEPTED"));

        Integer successCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where action = ? and result = ?",
                Integer.class,
                "OPEN_DOOR",
                "SUCCESS"
        );

        assertThat(successCount).isEqualTo(1);
    }

    @Test
    void shouldRejectReplayAttackAndPersistFailureAuditLog() throws Exception {
        Fixture fixture = createActiveSubscriptionFixture();

        String authToken = login(fixture.userId());
        String qrToken = issueQrToken(authToken, fixture.subscriptionId());

        mockMvc.perform(post("/entrances/open")
                        .header("Authorization", "Bearer " + authToken)
                        .header("X-Request-Id", "open-replay-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "qrToken": "%s"
                                }
                                """.formatted(qrToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/entrances/open")
                        .header("Authorization", "Bearer " + authToken)
                        .header("X-Request-Id", "open-replay-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "qrToken": "%s"
                                }
                                """.formatted(qrToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));

        Integer successCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where action = ? and result = ?",
                Integer.class,
                "OPEN_DOOR",
                "SUCCESS"
        );
        Integer failureCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where action = ? and result = ?",
                Integer.class,
                "OPEN_DOOR",
                "FAILURE"
        );

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);
    }

    private Fixture createActiveSubscriptionFixture() {
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
                "Debug User",
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
                null,
                null,
                now,
                now,
                now.plusSeconds(3600),
                null,
                null,
                now,
                now,
                0,
                false
        );

        return new Fixture(userId, subscriptionId);
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
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(content);
        return root.path("data").path("authToken").asText();
    }

    private String issueQrToken(String authToken, UUID subscriptionId) throws Exception {
        String content = mockMvc.perform(post("/entrances/qr-tokens/{subscriptionId}", subscriptionId)
                        .header("Authorization", "Bearer " + authToken)
                        .header("X-Request-Id", "issue-qr-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(content);
        return root.path("data").path("qrToken").asText();
    }

    private record Fixture(UUID userId, UUID subscriptionId) {
    }
}