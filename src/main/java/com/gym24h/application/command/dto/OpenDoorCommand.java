package com.gym24h.application.command.dto;

import java.util.UUID;

public record OpenDoorCommand(UUID userId, String qrToken) {
}
