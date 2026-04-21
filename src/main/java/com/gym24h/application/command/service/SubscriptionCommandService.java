package com.gym24h.application.command.service;

import com.gym24h.application.command.dto.CreateSubscriptionCommand;
import com.gym24h.application.query.dto.SubscriptionView;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.domain.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionCommandService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionCommandService(UserRepository userRepository, SubscriptionRepository subscriptionRepository) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public SubscriptionView create(CreateSubscriptionCommand command) {
        UserId userId = new UserId(command.userId());
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "User not found",
                        HttpStatus.NOT_FOUND
                ));

        Subscription subscription = Subscription.create(userId, command.billingCycle(), command.startsAt());
        Subscription saved = subscriptionRepository.save(subscription);
        return new SubscriptionView(
                saved.getId().value(),
                saved.getUserId().value(),
                saved.getBillingCycle(),
                saved.getStatus(),
                saved.getValidFrom(),
                saved.getValidUntil(),
                saved.getCancellationRequestedAt()
        );
    }
}
