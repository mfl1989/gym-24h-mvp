package com.gym24h.infrastructure.external.stripe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StripeWebhookHandler {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookHandler.class);

    public void handle(String eventId, String eventType, String payload) {
        log.info("stripe webhook received eventId={} eventType={} payloadSize={}", eventId, eventType, payload.length());
    }
}
