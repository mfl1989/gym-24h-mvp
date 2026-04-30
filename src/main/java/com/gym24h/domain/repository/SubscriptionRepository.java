package com.gym24h.domain.repository;

import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.subscription.SubscriptionId;
import com.gym24h.domain.model.user.UserId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {

    Optional<Subscription> findById(SubscriptionId subscriptionId);

    Optional<Subscription> findActiveByUserId(UserId userId);

    Optional<Subscription> findActiveOrArrearsByUserId(UserId userId);

    Optional<Subscription> findLatestByUserId(UserId userId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    List<Subscription> findExpiredActiveSubscriptions(Instant threshold);

    Subscription save(Subscription subscription);
}
