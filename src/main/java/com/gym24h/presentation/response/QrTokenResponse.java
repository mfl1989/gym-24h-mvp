package com.gym24h.presentation.response;

import java.time.Instant;
import java.util.UUID;

public record QrTokenResponse(
        String qrToken,
        UUID tokenId,
        Instant expiresAt
) {
}
