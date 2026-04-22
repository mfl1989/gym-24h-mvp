package com.gym24h.application.query.dto;

import com.gym24h.domain.model.subscription.SubscriptionStatus;

import java.time.Instant;

public record MySubscriptionDetailsView(
        SubscriptionStatus status,
        Instant currentPeriodStartAt,
        Instant currentPeriodEndAt,
        boolean cancelAtPeriodEnd,
        boolean withinCancellationWindow
) {
}
