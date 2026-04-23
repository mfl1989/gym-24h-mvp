package com.gym24h.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record AdminScanEntranceRequest(@NotBlank String qrToken) {
}