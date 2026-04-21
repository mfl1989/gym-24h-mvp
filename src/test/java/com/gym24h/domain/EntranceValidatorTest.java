package com.gym24h.domain;

import com.gym24h.domain.model.subscription.BillingCycle;
import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.service.EntranceValidator;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EntranceValidatorTest {

    private final EntranceValidator validator = new EntranceValidator();

    @Test
    void shouldAllowActiveSubscriptionWithinWindow() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Subscription subscription = Subscription.create(UserId.newId(), BillingCycle.THIRTY_DAYS, start);

        assertDoesNotThrow(() -> validator.validate(subscription, start.plusSeconds(60)));
    }

    @Test
    void shouldRejectExpiredSubscription() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Subscription subscription = Subscription.create(UserId.newId(), BillingCycle.THIRTY_DAYS, start);

        assertThrows(IllegalStateException.class,
                () -> validator.validate(subscription, start.plus(BillingCycle.THIRTY_DAYS.duration()).plusSeconds(12)));
    }
}
