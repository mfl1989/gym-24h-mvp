package com.gym24h.domain.event;

import java.time.Instant;

public record SubscriptionActivatedEvent(String aggregateId, Instant occurredAt) implements DomainEvent {

    @Override
    public String type() {
        return "subscription.activated";
    }
}
