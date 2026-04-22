package com.gym24h.presentation.response;

import java.time.Instant;

public record AuthTokenResponse(String authToken, Instant expiresAt) {
}
