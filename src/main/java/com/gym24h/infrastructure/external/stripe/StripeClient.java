package com.gym24h.infrastructure.external.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.time.Instant;
import java.util.List;

public interface StripeClient {

    Event verifyWebhookSignature(String payload, String signatureHeader) throws SignatureVerificationException;

    Session createCheckoutSession(SessionCreateParams params) throws StripeException;

    Subscription scheduleCancellationAtPeriodEnd(String stripeSubscriptionId) throws StripeException;

    Subscription revokeCancellationAtPeriodEnd(String stripeSubscriptionId) throws StripeException;

    List<Invoice> listInvoicesSince(Instant lookbackPeriod) throws StripeException;
}
