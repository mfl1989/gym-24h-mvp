package com.gym24h.domain.model.subscription;

import java.time.Duration;

public enum BillingCycle {
    THIRTY_DAYS(Duration.ofDays(30)),
    YEARLY(Duration.ofDays(365));

    private final Duration duration;

    BillingCycle(Duration duration) {
        this.duration = duration;
    }

    public Duration duration() {
        return duration;
    }
}
