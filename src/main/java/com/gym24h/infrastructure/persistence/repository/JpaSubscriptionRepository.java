package com.gym24h.infrastructure.persistence.repository;

import com.gym24h.domain.model.subscription.SubscriptionStatus;
import com.gym24h.infrastructure.persistence.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    Optional<SubscriptionEntity> findByIdAndDeletedFalse(UUID id);

    Optional<SubscriptionEntity> findFirstByUserIdAndStatusAndDeletedFalse(UUID userId, SubscriptionStatus status);

    Optional<SubscriptionEntity> findFirstByUserIdAndStatusInAndDeletedFalse(UUID userId, Collection<SubscriptionStatus> statuses);

    Optional<SubscriptionEntity> findFirstByUserIdAndDeletedFalseOrderByCurrentPeriodEndAtDesc(UUID userId);

    Optional<SubscriptionEntity> findFirstByStripeSubscriptionIdAndDeletedFalse(String stripeSubscriptionId);

    List<SubscriptionEntity> findByStatusAndCurrentPeriodEndAtBeforeAndDeletedFalse(SubscriptionStatus status, Instant threshold);
}

