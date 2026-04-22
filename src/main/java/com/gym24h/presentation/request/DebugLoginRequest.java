package com.gym24h.presentation.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DebugLoginRequest(@NotNull UUID userId) {
}
