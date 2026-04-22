package com.gym24h.application.query.dto;

import com.gym24h.domain.model.subscription.SubscriptionStatus;

import java.util.UUID;

public record AdminUserProfileView(
        UUID userId,
        String lineUserId,
        String displayName,
        String membershipStatus,
        SubscriptionStatus subscriptionStatus
) {
}
