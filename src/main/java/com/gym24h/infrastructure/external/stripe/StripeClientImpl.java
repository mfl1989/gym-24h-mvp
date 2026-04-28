package com.gym24h.infrastructure.external.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.net.RequestOptions;
import com.stripe.param.InvoiceListParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class StripeClientImpl implements StripeClient {

    private final String secretKey;
    private final String endpointSecret;

    public StripeClientImpl(
            @Value("${stripe.api.key:${stripe.api.secret-key:}}") String secretKey,
            @Value("${stripe.webhook.secret}") String endpointSecret
    ) {
        this.secretKey = secretKey;
        this.endpointSecret = endpointSecret;
    }

    @Override
    public Event verifyWebhookSignature(String payload, String signatureHeader) throws SignatureVerificationException {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new SignatureVerificationException("Stripe signature header is required", signatureHeader);
        }
        return Webhook.constructEvent(payload, signatureHeader, endpointSecret);
    }

    @Override
    public Session createCheckoutSession(SessionCreateParams params) throws StripeException {
        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(secretKey)
                .build();
        return Session.create(params, requestOptions);
    }

        @Override
        public Subscription scheduleCancellationAtPeriodEnd(String stripeSubscriptionId) throws StripeException {
        RequestOptions requestOptions = RequestOptions.builder()
            .setApiKey(secretKey)
            .build();
        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
            .setCancelAtPeriodEnd(true)
            .build();
        return Subscription.retrieve(stripeSubscriptionId, requestOptions).update(params, requestOptions);
        }

        @Override
        public Subscription revokeCancellationAtPeriodEnd(String stripeSubscriptionId) throws StripeException {
        RequestOptions requestOptions = RequestOptions.builder()
            .setApiKey(secretKey)
            .build();
        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
            .setCancelAtPeriodEnd(false)
            .build();
        return Subscription.retrieve(stripeSubscriptionId, requestOptions).update(params, requestOptions);
        }

    @Override
    public List<Invoice> listInvoicesSince(Instant lookbackPeriod) throws StripeException {
        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(secretKey)
                .build();
        InvoiceListParams params = InvoiceListParams.builder()
                .setLimit(100L)
                .setCreated(InvoiceListParams.Created.builder()
                        .setGte(lookbackPeriod.getEpochSecond())
                        .build())
                .build();

        InvoiceCollection invoices = Invoice.list(params, requestOptions);
        List<Invoice> results = new ArrayList<>();
        for (Invoice invoice : invoices.autoPagingIterable()) {
            results.add(invoice);
        }
        return results;
    }
}