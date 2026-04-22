package com.gym24h.presentation.controller;

import com.gym24h.application.command.dto.CreateSubscriptionCommand;
import com.gym24h.application.command.service.SubscriptionCommandService;
import com.gym24h.application.query.service.SubscriptionQueryService;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.infrastructure.security.AuthenticatedUser;
import com.gym24h.presentation.request.CheckoutSessionRequest;
import com.gym24h.presentation.request.CreateSubscriptionRequest;
import com.gym24h.presentation.response.ApiResponse;
import com.gym24h.presentation.response.CheckoutSessionResponse;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @PostMapping("/checkout-sessions")
    public ApiResponse<CheckoutSessionResponse> createCheckoutSession(
            @Validated @RequestBody CheckoutSessionRequest request
    ) {
        String checkoutUrl = subscriptionCommandService.createCheckoutSession(currentUserId(), request.priceId());
        return ApiResponse.ok(new CheckoutSessionResponse(checkoutUrl));
    }

    @PostMapping("/webhooks")
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        try {
            Event event = subscriptionCommandService.verifyWebhookSignature(payload, signatureHeader);
            subscriptionCommandService.handleVerifiedWebhookEventAsync(event);
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException exception) {
            return ResponseEntity.badRequest().build();
        }
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

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(
                    ErrorCodes.UNAUTHORIZED,
                    "Authentication is required",
                    HttpStatus.UNAUTHORIZED
            );
        }
        return authenticatedUser.userId();
    }
}
