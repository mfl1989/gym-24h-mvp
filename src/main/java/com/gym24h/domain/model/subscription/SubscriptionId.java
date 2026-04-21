package com.gym24h.domain.model.subscription;

import java.util.UUID;

public record SubscriptionId(UUID value) {

    public SubscriptionId {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }

    public static SubscriptionId newId() {
        return new SubscriptionId(UUID.randomUUID());
    }
}
