package com.gym24h.application.service;

import com.gym24h.application.command.service.SubscriptionExpirationBatchService;
import com.gym24h.domain.model.subscription.BillingCycle;
import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.subscription.SubscriptionStatus;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.infrastructure.persistence.repository.AuditLogJdbcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirationBatchServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private AuditLogJdbcRepository auditLogJdbcRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private SubscriptionExpirationBatchService subscriptionExpirationBatchService;

    @Test
    void shouldExpireSubscriptionAndPersistAuditLog() {
        Instant now = Instant.parse("2026-04-30T00:05:00Z");
        UUID userId = UUID.randomUUID();
        Subscription subscription = spy(createActiveSubscription(userId, now.minusSeconds(3600), now.minusSeconds(1)));

        when(subscriptionRepository.findExpiredActiveSubscriptions(now)).thenReturn(List.of(subscription));
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);

        int expiredCount = subscriptionExpirationBatchService.expireSubscriptions(now);

        assertThat(expiredCount).isEqualTo(1);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(subscriptionRepository).save(subscription);

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);

        verify(auditLogJdbcRepository).save(
                userIdCaptor.capture(),
                actionCaptor.capture(),
                resultCaptor.capture(),
                reasonCaptor.capture(),
                eq(null)
        );

        assertThat(userIdCaptor.getValue()).isEqualTo(userId);
        assertThat(actionCaptor.getValue()).isEqualTo("SYSTEM_EXPIRATION");
        assertThat(resultCaptor.getValue()).isEqualTo("SUCCESS");
        assertThat(reasonCaptor.getValue()).isEqualTo("Subscription expired by batch");
    }

    @Test
    void shouldSkipPersistenceWhenSubscriptionRemainsActive() {
        Instant now = Instant.parse("2026-04-30T00:05:00Z");
        Subscription subscription = spy(createActiveSubscription(UUID.randomUUID(), now.minusSeconds(3600), now.plusMillis(1)));

        when(subscriptionRepository.findExpiredActiveSubscriptions(now)).thenReturn(List.of(subscription));

        int expiredCount = subscriptionExpirationBatchService.expireSubscriptions(now);

        assertThat(expiredCount).isZero();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository, never()).save(any());
        verify(auditLogJdbcRepository, never()).save(any(), any(), any(), any(), any());
    }

    @Test
    void scheduledExpirationShouldUseClockInstant() {
        Instant now = Instant.parse("2026-04-30T00:05:00Z");
        Clock fixedClock = Clock.fixed(now, ZoneOffset.UTC);
        SubscriptionExpirationBatchService service = new SubscriptionExpirationBatchService(
                subscriptionRepository,
                auditLogJdbcRepository,
                fixedClock
        );

        when(subscriptionRepository.findExpiredActiveSubscriptions(now)).thenReturn(List.of());

        service.scheduledExpiration();

        verify(subscriptionRepository).findExpiredActiveSubscriptions(now);
    }

    private Subscription createActiveSubscription(UUID userId, Instant currentPeriodStartAt, Instant currentPeriodEndAt) {
        Instant createdAt = currentPeriodStartAt.minusSeconds(60);
        return Subscription.builder()
                .id(com.gym24h.domain.model.subscription.SubscriptionId.newId())
                .userId(new UserId(userId))
                .planCode("STANDARD")
                .billingCycle(BillingCycle.THIRTY_DAYS)
                .status(SubscriptionStatus.ACTIVE)
                .stripeCustomerId("cus_" + userId)
                .stripeSubscriptionId("sub_" + userId)
                .startedAt(currentPeriodStartAt)
                .currentPeriodStartAt(currentPeriodStartAt)
                .currentPeriodEndAt(currentPeriodEndAt)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .version(0)
                .deleted(false)
                .build();
    }
}