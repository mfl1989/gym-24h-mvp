package com.gym24h.application.query.dto;

import com.gym24h.domain.model.subscription.SubscriptionStatus;

import java.time.Instant;

public record CurrentUserProfileView(
        String displayName,
        String membershipStatus,
        SubscriptionStatus subscriptionStatus,
        Instant subscriptionValidUntil,
        boolean cancelAtPeriodEnd
) {
}
