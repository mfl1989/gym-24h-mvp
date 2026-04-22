package com.gym24h.presentation.response;

import com.gym24h.domain.model.subscription.SubscriptionStatus;

import java.time.Instant;

public record MySubscriptionDetailsResponse(
        SubscriptionStatus status,
        Instant currentPeriodStartAt,
        Instant currentPeriodEndAt,
        boolean cancelAtPeriodEnd,
        boolean withinCancellationWindow
) {
}
