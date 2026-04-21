package com.gym24h.presentation.controller;

import com.gym24h.application.command.dto.CreateSubscriptionCommand;
import com.gym24h.application.command.service.SubscriptionCommandService;
import com.gym24h.application.query.service.SubscriptionQueryService;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.presentation.request.CreateSubscriptionRequest;
import com.gym24h.presentation.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;

    public SubscriptionController(
            SubscriptionCommandService subscriptionCommandService,
            SubscriptionQueryService subscriptionQueryService
    ) {
        this.subscriptionCommandService = subscriptionCommandService;
        this.subscriptionQueryService = subscriptionQueryService;
    }

    @PostMapping
    public ApiResponse<?> create(@Validated @RequestBody CreateSubscriptionRequest request) {
        return ApiResponse.ok(subscriptionCommandService.create(new CreateSubscriptionCommand(
                request.userId(),
                request.billingCycle(),
                request.startsAt()
        )));
    }

    @GetMapping("/{subscriptionId}")
    public ApiResponse<?> findById(@PathVariable UUID subscriptionId) {
        return ApiResponse.ok(subscriptionQueryService.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "Subscription not found",
                        HttpStatus.NOT_FOUND
                )));
    }
}
