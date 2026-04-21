package com.gym24h.application.query.dto;

import com.gym24h.domain.model.subscription.BillingCycle;
import com.gym24h.domain.model.subscription.SubscriptionStatus;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionView(
        UUID id,
        UUID userId,
        BillingCycle billingCycle,
        SubscriptionStatus status,
        Instant validFrom,
        Instant validUntil,
        Instant cancellationRequestedAt
) {
}
