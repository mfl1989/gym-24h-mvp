package com.gym24h.presentation.controller;

import com.gym24h.application.command.service.SubscriptionCommandService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final SubscriptionCommandService subscriptionCommandService;

    public WebhookController(SubscriptionCommandService subscriptionCommandService) {
        this.subscriptionCommandService = subscriptionCommandService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> receiveStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        try {
            Event event = subscriptionCommandService.verifyWebhookSignature(payload, signatureHeader);
            subscriptionCommandService.handleVerifiedWebhookEventAsync(event);
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
