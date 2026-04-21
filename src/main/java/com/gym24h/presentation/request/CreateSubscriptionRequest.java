package com.gym24h.presentation.request;

import com.gym24h.domain.model.subscription.BillingCycle;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateSubscriptionRequest(
        @NotNull UUID userId,
        @NotNull BillingCycle billingCycle,
        @NotNull Instant startsAt
) {
}
