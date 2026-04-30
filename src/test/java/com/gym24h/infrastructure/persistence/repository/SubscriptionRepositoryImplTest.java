package com.gym24h.infrastructure.persistence.repository;

import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SubscriptionRepositoryImplTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

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
    void shouldFindOnlyExpiredActiveSubscriptions() {
        Instant now = Instant.parse("2026-04-30T00:05:00Z");

        UUID expiredActiveUserId = insertUser("Expired Active User");
        UUID expiredActiveSubscriptionId = insertSubscription(expiredActiveUserId, "ACTIVE", now.minusSeconds(60));

        UUID activeFutureUserId = insertUser("Active Future User");
        insertSubscription(activeFutureUserId, "ACTIVE", now.plusSeconds(3600));

        UUID arrearsExpiredUserId = insertUser("Arrears Expired User");
        insertSubscription(arrearsExpiredUserId, "ARREARS", now.minusSeconds(60));

        List<Subscription> expiredSubscriptions = subscriptionRepository.findExpiredActiveSubscriptions(now);

        assertThat(expiredSubscriptions)
                .hasSize(1)
                .extracting(subscription -> subscription.getId().value())
                .containsExactly(expiredActiveSubscriptionId);

        Subscription expiredSubscription = expiredSubscriptions.getFirst();
        assertThat(expiredSubscription.getUserId().value()).isEqualTo(expiredActiveUserId);
        assertThat(expiredSubscription.getStatus().name()).isEqualTo("ACTIVE");
    }

    private UUID insertUser(String displayName) {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-29T00:00:00Z");

        jdbcTemplate.update(
                """
                insert into users (id, line_user_id, phone_number, display_name, membership_status, created_at, updated_at, version, is_deleted)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                userId,
                "line-" + userId,
                null,
                displayName,
                "ACTIVE",
                now,
                now,
                0,
                false
        );

        return userId;
    }

    private UUID insertSubscription(UUID userId, String status, Instant currentPeriodEndAt) {
        UUID subscriptionId = UUID.randomUUID();
        Instant startedAt = currentPeriodEndAt.minusSeconds(30L * 24L * 60L * 60L);
        Instant now = Instant.parse("2026-04-29T00:00:00Z");

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
                "cus_" + userId,
                "sub_" + subscriptionId,
                startedAt,
                startedAt,
                currentPeriodEndAt,
                null,
                null,
                now,
                now,
                0,
                false
        );

        return subscriptionId;
    }
}