package com.gym24h.domain.service;

import com.gym24h.domain.model.subscription.Subscription;

import java.time.Instant;

public class SubscriptionDomainService {

    public Subscription markPaymentFailed(Subscription subscription) {
        subscription.markArrears();
        return subscription;
    }

    public Subscription recoverFromArrears(Subscription subscription) {
        subscription.markActive();
        return subscription;
    }

    public Subscription requestCancellation(Subscription subscription, Instant requestedAt) {
        subscription.requestCancellation(requestedAt);
        return subscription;
    }
}
