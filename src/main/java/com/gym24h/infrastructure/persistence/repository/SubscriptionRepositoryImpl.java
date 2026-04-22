package com.gym24h.infrastructure.persistence.repository;

import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.subscription.SubscriptionId;
import com.gym24h.domain.model.subscription.SubscriptionStatus;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.infrastructure.persistence.entity.SubscriptionEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Repository
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final JpaSubscriptionRepository jpaSubscriptionRepository;

    public SubscriptionRepositoryImpl(JpaSubscriptionRepository jpaSubscriptionRepository) {
        this.jpaSubscriptionRepository = jpaSubscriptionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findById(SubscriptionId subscriptionId) {
        return jpaSubscriptionRepository.findByIdAndDeletedFalse(subscriptionId.value())
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findActiveByUserId(UserId userId) {
        return jpaSubscriptionRepository.findFirstByUserIdAndStatusAndDeletedFalse(userId.value(), SubscriptionStatus.ACTIVE)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findActiveOrArrearsByUserId(UserId userId) {
        return jpaSubscriptionRepository.findFirstByUserIdAndStatusInAndDeletedFalse(
                        userId.value(),
                        List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.ARREARS)
                )
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findLatestByUserId(UserId userId) {
        return jpaSubscriptionRepository.findFirstByUserIdAndDeletedFalseOrderByCurrentPeriodEndAtDesc(userId.value())
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return jpaSubscriptionRepository.findFirstByStripeSubscriptionIdAndDeletedFalse(stripeSubscriptionId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public Subscription save(Subscription subscription) {
        SubscriptionEntity savedEntity = jpaSubscriptionRepository.save(toEntity(subscription));
        return toDomain(savedEntity);
    }

    private Subscription toDomain(SubscriptionEntity entity) {
        return Subscription.builder()
                .id(new SubscriptionId(entity.getId()))
                .userId(new UserId(entity.getUserId()))
                .planCode(entity.getPlanCode())
                .billingCycle(entity.getBillingCycle())
                .status(entity.getStatus())
                .stripeCustomerId(entity.getStripeCustomerId())
                .stripeSubscriptionId(entity.getStripeSubscriptionId())
                .startedAt(entity.getStartedAt())
                .currentPeriodStartAt(entity.getCurrentPeriodStartAt())
                .currentPeriodEndAt(entity.getCurrentPeriodEndAt())
                .canceledAt(entity.getCanceledAt())
                .cancellationRequestedAt(entity.getCancellationRequestedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .deleted(entity.isDeleted())
                .subChar1(entity.getSubChar1())
                .subChar2(entity.getSubChar2())
                .subChar3(entity.getSubChar3())
                .subChar4(entity.getSubChar4())
                .subChar5(entity.getSubChar5())
                .subChar6(entity.getSubChar6())
                .subChar7(entity.getSubChar7())
                .subChar8(entity.getSubChar8())
                .subChar9(entity.getSubChar9())
                .subChar10(entity.getSubChar10())
                .build();
    }

    private SubscriptionEntity toEntity(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity();
        entity.setId(subscription.getId().value());
        entity.setUserId(subscription.getUserId().value());
        entity.setPlanCode(subscription.getPlanCode());
        entity.setBillingCycle(subscription.getBillingCycle());
        entity.setStatus(subscription.getStatus());
        entity.setStripeCustomerId(subscription.getStripeCustomerId());
        entity.setStripeSubscriptionId(subscription.getStripeSubscriptionId());
        entity.setStartedAt(subscription.getStartedAt());
        entity.setCurrentPeriodStartAt(subscription.getCurrentPeriodStartAt());
        entity.setCurrentPeriodEndAt(subscription.getCurrentPeriodEndAt());
        entity.setCanceledAt(subscription.getCanceledAt());
        entity.setCancellationRequestedAt(subscription.getCancellationRequestedAt());
        entity.setCreatedAt(subscription.getCreatedAt());
        entity.setUpdatedAt(subscription.getUpdatedAt());
        entity.setVersion(subscription.getVersion());
        entity.setDeleted(subscription.isDeleted());
        entity.setSubChar1(subscription.getSubChar1());
        entity.setSubChar2(subscription.getSubChar2());
        entity.setSubChar3(subscription.getSubChar3());
        entity.setSubChar4(subscription.getSubChar4());
        entity.setSubChar5(subscription.getSubChar5());
        entity.setSubChar6(subscription.getSubChar6());
        entity.setSubChar7(subscription.getSubChar7());
        entity.setSubChar8(subscription.getSubChar8());
        entity.setSubChar9(subscription.getSubChar9());
        entity.setSubChar10(subscription.getSubChar10());
        return entity;
    }
}