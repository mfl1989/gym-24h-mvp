package com.gym24h.application.command.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record StripeEvent(
        @JsonProperty("id") String eventId,
        String type,
        StripeEventData data
) {

    public record StripeEventData(StripeEventObject object) {
    }

    public record StripeEventObject(
            String id,
            String customer,
            @JsonProperty("subscription") String subscriptionId,
            Map<String, String> metadata
    ) {
    }
}
