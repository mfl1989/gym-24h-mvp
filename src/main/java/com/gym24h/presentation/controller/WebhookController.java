package com.gym24h.presentation.controller;

import com.gym24h.infrastructure.external.stripe.StripeWebhookHandler;
import com.gym24h.presentation.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final StripeWebhookHandler stripeWebhookHandler;

    public WebhookController(StripeWebhookHandler stripeWebhookHandler) {
        this.stripeWebhookHandler = stripeWebhookHandler;
    }

    @PostMapping("/stripe")
    public ApiResponse<String> receiveStripeWebhook(
            @RequestHeader("Stripe-Event-Id") String eventId,
            @RequestHeader("Stripe-Event-Type") String eventType,
            @RequestBody String payload
    ) {
        stripeWebhookHandler.handle(eventId, eventType, payload);
        return ApiResponse.ok("WEBHOOK_ACCEPTED");
    }
}
