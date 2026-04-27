package com.gym24h.presentation.controller;

import com.gym24h.application.command.service.SubscriptionCommandService;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.infrastructure.security.AuthenticatedUser;
import com.gym24h.presentation.response.ApiResponse;
import com.gym24h.presentation.response.CheckoutSessionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final SubscriptionCommandService subscriptionCommandService;
    private final String stripePriceId;

    public PaymentController(
            SubscriptionCommandService subscriptionCommandService,
            @Value("${stripe.checkout.price-id:YOUR_STRIPE_PRICE_ID}") String stripePriceId
    ) {
        this.subscriptionCommandService = subscriptionCommandService;
        this.stripePriceId = stripePriceId;
    }

    @PostMapping("/create-checkout-session")
    public ApiResponse<CheckoutSessionResponse> createCheckoutSession() {
        if (isMissingOrPlaceholder(stripePriceId, "YOUR_STRIPE_PRICE_ID")) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "stripe.checkout.price-id is not configured");
        }

        String checkoutUrl = subscriptionCommandService.createCheckoutSession(currentUserId(), stripePriceId);
        return ApiResponse.ok(new CheckoutSessionResponse(checkoutUrl));
    }

    private boolean isMissingOrPlaceholder(String value, String placeholderPrefix) {
        return value == null || value.isBlank() || value.startsWith(placeholderPrefix);
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