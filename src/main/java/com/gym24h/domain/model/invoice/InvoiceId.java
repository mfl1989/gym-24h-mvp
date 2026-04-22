package com.gym24h.domain.model.invoice;

import java.util.UUID;

public record InvoiceId(UUID value) {

    public InvoiceId {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }

    public static InvoiceId newId() {
        return new InvoiceId(UUID.randomUUID());
    }
}
