package com.gym24h.application.command.service;

import com.gym24h.application.command.dto.OpenDoorCommand;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.domain.model.subscription.SubscriptionId;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.domain.service.EntranceValidator;
import com.gym24h.infrastructure.cache.redis.IdempotencyService;
import com.gym24h.infrastructure.external.iot.DoorLockClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EntranceCommandService {

    private final SubscriptionRepository subscriptionRepository;
    private final EntranceValidator entranceValidator;
    private final IdempotencyService idempotencyService;
    private final DoorLockClient doorLockClient;

    public EntranceCommandService(
            SubscriptionRepository subscriptionRepository,
            EntranceValidator entranceValidator,
            IdempotencyService idempotencyService,
            DoorLockClient doorLockClient
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.entranceValidator = entranceValidator;
        this.idempotencyService = idempotencyService;
        this.doorLockClient = doorLockClient;
    }

    public void openDoor(OpenDoorCommand command) {
        boolean accepted = idempotencyService.acquire("door:" + command.requestId(), 5);
        if (!accepted) {
            throw new BusinessException(
                    ErrorCodes.DUPLICATE_REQUEST,
                    "Duplicate door request",
                    HttpStatus.CONFLICT
            );
        }

        var subscription = subscriptionRepository.findById(new SubscriptionId(command.subscriptionId()))
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "Subscription not found",
                        HttpStatus.NOT_FOUND
                ));

        if (!subscription.getUserId().value().equals(command.userId())) {
            throw new BusinessException(
                    ErrorCodes.BUSINESS_RULE_VIOLATION,
                    "Subscription does not belong to the user",
                    HttpStatus.BAD_REQUEST
            );
        }

        entranceValidator.validate(subscription, command.requestedAt());

        if (!doorLockClient.open(subscription.getUserId().value().toString(), command.requestId())) {
            throw new BusinessException(
                    ErrorCodes.INTERNAL_ERROR,
                    "Door open failed. Please retry later",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
    }
}
