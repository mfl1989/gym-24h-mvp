package com.gym24h.domain.event;

import java.time.Instant;

public interface DomainEvent {

    String aggregateId();

    Instant occurredAt();

    String type();
}
