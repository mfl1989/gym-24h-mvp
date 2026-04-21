package com.gym24h.domain.repository;

import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.subscription.SubscriptionId;
import com.gym24h.domain.model.user.UserId;

import java.util.Optional;

public interface SubscriptionRepository {

    Optional<Subscription> findById(SubscriptionId subscriptionId);

    Optional<Subscription> findActiveByUserId(UserId userId);

    Subscription save(Subscription subscription);
}
