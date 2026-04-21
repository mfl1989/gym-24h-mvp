package com.gym24h.application.query.service;

import com.gym24h.application.query.dto.SubscriptionView;
import com.gym24h.domain.model.subscription.SubscriptionId;
import com.gym24h.domain.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionQueryService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionQueryService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Optional<SubscriptionView> findById(UUID subscriptionId) {
        return subscriptionRepository.findById(new SubscriptionId(subscriptionId))
                .map(subscription -> new SubscriptionView(
                        subscription.getId().value(),
                        subscription.getUserId().value(),
                        subscription.getBillingCycle(),
                        subscription.getStatus(),
                        subscription.getValidFrom(),
                        subscription.getValidUntil(),
                        subscription.getCancellationRequestedAt()
                ));
    }
}
