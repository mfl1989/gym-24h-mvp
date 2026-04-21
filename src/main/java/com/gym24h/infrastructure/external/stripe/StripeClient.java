package com.gym24h.infrastructure.external.stripe;

public interface StripeClient {

    void verifyWebhookSignature(String payload, String signatureHeader);
}
