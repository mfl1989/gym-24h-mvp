package com.gym24h.infrastructure.security;

import java.time.Instant;
import java.util.UUID;

public record QrTokenClaims(
        UUID userId,
        UUID subscriptionId,
        String tokenId,
        Instant expiresAt
) {
}
