package com.gym24h.domain;

import com.gym24h.domain.model.subscription.BillingCycle;
import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.subscription.SubscriptionStatus;
import com.gym24h.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionTest {

    @Test
    void shouldCreateActiveSubscription() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        Subscription subscription = Subscription.create(UserId.newId(), BillingCycle.THIRTY_DAYS, start);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(start.plus(BillingCycle.THIRTY_DAYS.duration()), subscription.getValidUntil());
    }

    @Test
    void shouldMoveFromActiveToArrears() {
        Subscription subscription = Subscription.create(UserId.newId(), BillingCycle.THIRTY_DAYS, Instant.now());

        subscription.markArrears();

        assertEquals(SubscriptionStatus.ARREARS, subscription.getStatus());
    }

    @Test
    void shouldRejectIllegalStatusJump() {
        Subscription subscription = Subscription.create(UserId.newId(), BillingCycle.THIRTY_DAYS, Instant.now());

        assertThrows(IllegalStateException.class, subscription::markActive);
    }

    @Test
    void shouldExpireAfterBuffer() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Subscription subscription = Subscription.create(UserId.newId(), BillingCycle.THIRTY_DAYS, start);

        boolean eligible = subscription.canEnter(start.plus(BillingCycle.THIRTY_DAYS.duration()).plusSeconds(11), 10);

        assertTrue(!eligible);
        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
    }
}
