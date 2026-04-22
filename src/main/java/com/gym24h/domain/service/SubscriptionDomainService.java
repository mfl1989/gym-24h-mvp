package com.gym24h.domain.service;

import com.gym24h.domain.model.subscription.Subscription;

import java.time.Instant;

public class SubscriptionDomainService {

    public Subscription markPaymentFailed(Subscription subscription) {
        if (subscription.getStatus() == com.gym24h.domain.model.subscription.SubscriptionStatus.ACTIVE) {
            subscription.markArrears();
        }
        return subscription;
    }

    public Subscription recoverFromArrears(Subscription subscription) {
        subscription.markActive();
        return subscription;
    }

    public Subscription handleRecurringPaymentSuccess(Subscription subscription, Instant paidAt) {
        if (subscription.getStatus() == com.gym24h.domain.model.subscription.SubscriptionStatus.ARREARS) {
            subscription.markActive();
        } else if (subscription.getStatus() != com.gym24h.domain.model.subscription.SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE or ARREARS subscriptions can handle recurring payment success: current=" + subscription.getStatus());
        }
        subscription.extendCurrentPeriodFrom(paidAt);
        return subscription;
    }

    public Subscription handleRecurringPaymentFailure(Subscription subscription) {
        if (subscription.getStatus() == com.gym24h.domain.model.subscription.SubscriptionStatus.ACTIVE) {
            subscription.markArrears();
            return subscription;
        }
        if (subscription.getStatus() == com.gym24h.domain.model.subscription.SubscriptionStatus.ARREARS) {
            return subscription;
        }
        throw new IllegalStateException("Only ACTIVE or ARREARS subscriptions can handle recurring payment failure: current=" + subscription.getStatus());
    }

    public Subscription requestCancellation(Subscription subscription, Instant requestedAt) {
        subscription.requestCancellation(requestedAt);
        return subscription;
    }

    public Subscription revokeCancellation(Subscription subscription) {
        subscription.revokeCancellation();
        return subscription;
    }

    public Subscription forceTerminate(Subscription subscription, Instant terminatedAt) {
        subscription.forceTerminate(terminatedAt);
        return subscription;
    }
}
