package com.gym24h.infrastructure.persistence.repository;

import com.gym24h.domain.model.subscription.SubscriptionStatus;
import com.gym24h.infrastructure.persistence.entity.SubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaSubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {

    Optional<SubscriptionEntity> findByIdAndDeletedFalse(UUID id);

    Optional<SubscriptionEntity> findFirstByUserIdAndStatusAndDeletedFalse(UUID userId, SubscriptionStatus status);
}

