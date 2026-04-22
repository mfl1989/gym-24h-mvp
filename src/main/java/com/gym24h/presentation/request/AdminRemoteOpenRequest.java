package com.gym24h.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record AdminRemoteOpenRequest(
        @NotBlank String deviceId
) {
}
