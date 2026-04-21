package com.gym24h.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record OpenDoorRequest(
        @NotNull UUID userId,
        @NotNull UUID subscriptionId,
        @NotBlank String requestId,
        @NotNull Instant requestedAt
) {
}
