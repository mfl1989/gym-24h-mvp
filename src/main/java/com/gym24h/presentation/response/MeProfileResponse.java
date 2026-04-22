package com.gym24h.presentation.response;

import com.gym24h.domain.model.subscription.SubscriptionStatus;

import java.time.Instant;

public record MeProfileResponse(
        String displayName,
        String membershipStatus,
        SubscriptionStatus subscriptionStatus,
        Instant subscriptionValidUntil
) {
}
