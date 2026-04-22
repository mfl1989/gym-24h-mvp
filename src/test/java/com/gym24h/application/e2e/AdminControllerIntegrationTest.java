package com.gym24h.application.e2e;

import com.gym24h.application.outbound.DoorLockClient;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIntegrationTest {

    private static final String ADMIN_TOKEN = "super-secret-admin-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DoorLockClient doorLockClient;

    @BeforeEach
    void setUp() {
        doNothing().when(doorLockClient).unlock("MAIN_DOOR_99");
        jdbcTemplate.update("delete from audit_logs");
        jdbcTemplate.update("delete from invoices");
        jdbcTemplate.update("delete from subscriptions");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void shouldRejectAdminRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnAllUsersWithSubscriptionStatus() throws Exception {
        Fixture fixture = createUserWithActiveSubscription();

        mockMvc.perform(get("/admin/users")
                        .header("X-Admin-Token", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(fixture.userId().toString()))
                .andExpect(jsonPath("$.data[0].lineUserId").value("line-" + fixture.userId()))
                .andExpect(jsonPath("$.data[0].subscriptionStatus").value("ACTIVE"));
    }

    @Test
    void shouldRemoteOpenDoorAndPersistAuditLog() throws Exception {
        mockMvc.perform(post("/admin/entrances/remote-open")
                        .header("X-Admin-Token", ADMIN_TOKEN)
                        .header("X-Request-Id", "admin-open-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"deviceId\": \"MAIN_DOOR_99\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("REMOTE_OPEN_ACCEPTED"));

        verify(doorLockClient).unlock("MAIN_DOOR_99");

        Integer successCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where action = ? and result = ? and request_id = ?",
                Integer.class,
                "ADMIN_REMOTE_OPEN",
                "SUCCESS",
                "admin-open-1"
        );
        assertThat(successCount).isEqualTo(1);
    }

    @Test
    void shouldForceTerminateSubscriptionAndPersistAuditLog() throws Exception {
        Fixture fixture = createUserWithActiveSubscription();

        mockMvc.perform(post("/admin/subscriptions/{userId}/terminate", fixture.userId())
                        .header("X-Admin-Token", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"reason\": \"manual safety shutdown\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("FORCE_TERMINATION_ACCEPTED"));

        String subscriptionStatus = jdbcTemplate.queryForObject(
                "select status from subscriptions where id = ?",
                String.class,
                fixture.subscriptionId()
        );
        Integer auditCount = jdbcTemplate.queryForObject(
                "select count(*) from audit_logs where user_id = ? and action = ? and result = ? and reason = ?",
                Integer.class,
                fixture.userId(),
                "FORCE_TERMINATED",
                "SUCCESS",
                "manual safety shutdown"
        );

        assertThat(subscriptionStatus).isEqualTo("EXPIRED");
        assertThat(auditCount).isEqualTo(1);
    }

    private Fixture createUserWithActiveSubscription() {
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
                "Admin Fixture User",
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

    private record Fixture(UUID userId, UUID subscriptionId) {
    }
}
