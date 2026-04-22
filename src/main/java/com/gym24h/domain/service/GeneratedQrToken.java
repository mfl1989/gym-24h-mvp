package com.gym24h.domain.service;

import java.time.Instant;
import java.util.UUID;

public record GeneratedQrToken(
        String token,
        UUID tokenId,
        Instant expiresAt
) {
}
