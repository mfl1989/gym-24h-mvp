package com.gym24h.presentation.response;

import java.time.Instant;

public record ErrorResponse(String code, String message, Instant timestamp, String requestId) {
}
