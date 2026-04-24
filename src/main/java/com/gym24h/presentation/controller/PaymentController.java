package com.gym24h.presentation.controller;

import com.gym24h.presentation.response.ApiResponse;
import com.gym24h.presentation.response.CheckoutSessionResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final String SUCCESS_URL = "http://localhost:5173/dashboard?payment=success";
    private static final String CANCEL_URL = "http://localhost:5173/dashboard?payment=cancel";
    private static final String PRICE_ID = "price_1TPd0b0mDQ7xu2QmGuuysMwr";

    private final String stripeApiKey;

    public PaymentController(@Value("${stripe.api.key:${stripe.api.secret-key:}}") String stripeApiKey) {
        this.stripeApiKey = stripeApiKey;
    }

    @PostMapping("/create-checkout-session")
    public ApiResponse<CheckoutSessionResponse> createCheckoutSession() {
        if (stripeApiKey == null || stripeApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "stripe.api.key is not configured");
        }

        Stripe.apiKey = stripeApiKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(SUCCESS_URL)
                .setCancelUrl(CANCEL_URL)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPrice(PRICE_ID)
                        .build())
                .build();

        try {
            Session session = Session.create(params);
            return ApiResponse.ok(new CheckoutSessionResponse(session.getUrl()));
        } catch (StripeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to create Stripe checkout session",
                    exception
            );
        }
    }
}