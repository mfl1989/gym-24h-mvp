package com.gym24h.presentation.response;

import com.gym24h.domain.model.subscription.SubscriptionStatus;

import java.util.UUID;

public record AdminUserProfileResponse(
        UUID userId,
        String lineUserId,
        String displayName,
        String membershipStatus,
        SubscriptionStatus subscriptionStatus
) {
}
