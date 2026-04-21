package com.gym24h.application.command.dto;

import java.time.Instant;
import java.util.UUID;

public record OpenDoorCommand(UUID userId, UUID subscriptionId, String requestId, Instant requestedAt) {
}
