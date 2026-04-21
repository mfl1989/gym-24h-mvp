package com.gym24h.domain.service;

import com.gym24h.domain.model.subscription.Subscription;

import java.time.Instant;

public class EntranceValidator {

    private static final long DEFAULT_BUFFER_SECONDS = 10L;

    public void validate(Subscription subscription, Instant requestedAt) {
        if (!subscription.canEnter(requestedAt, DEFAULT_BUFFER_SECONDS)) {
            throw new IllegalStateException("Subscription is not eligible for entrance");
        }
    }
}
