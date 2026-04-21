package com.gym24h.domain.model.user;

import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}
