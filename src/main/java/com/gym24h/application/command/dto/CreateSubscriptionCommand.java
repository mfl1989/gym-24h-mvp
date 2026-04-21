package com.gym24h.application.command.dto;

import com.gym24h.domain.model.subscription.BillingCycle;

import java.time.Instant;
import java.util.UUID;

public record CreateSubscriptionCommand(UUID userId, BillingCycle billingCycle, Instant startsAt) {
}
