package com.gym24h.presentation.controller;

import com.gym24h.application.command.service.SubscriptionCommandService;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.infrastructure.security.AuthenticatedUser;
import com.gym24h.presentation.response.ApiResponse;
import com.gym24h.presentation.response.CheckoutSessionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateCheckoutSessionThroughSubscriptionCommandService() {
        SubscriptionCommandService subscriptionCommandService = mock(SubscriptionCommandService.class);
        UUID userId = UUID.randomUUID();
        String checkoutUrl = "https://checkout.stripe.test/session_123";

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthenticatedUser(userId), null)
        );
        when(subscriptionCommandService.createCheckoutSession(userId, "price_test_123"))
                .thenReturn(checkoutUrl);

        PaymentController controller = new PaymentController(subscriptionCommandService, "price_test_123");

        ApiResponse<CheckoutSessionResponse> response = controller.createCheckoutSession();

        assertThat(response.success()).isTrue();
        assertThat(response.data().checkoutUrl()).isEqualTo(checkoutUrl);
        verify(subscriptionCommandService).createCheckoutSession(userId, "price_test_123");
    }

    @Test
    void shouldRejectWhenPriceIdIsPlaceholder() {
        SubscriptionCommandService subscriptionCommandService = mock(SubscriptionCommandService.class);
        PaymentController controller = new PaymentController(subscriptionCommandService, "YOUR_STRIPE_PRICE_ID");

        assertThatThrownBy(controller::createCheckoutSession)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });

        verifyNoInteractions(subscriptionCommandService);
    }

    @Test
    void shouldRejectWhenAuthenticationIsMissing() {
        SubscriptionCommandService subscriptionCommandService = mock(SubscriptionCommandService.class);
        PaymentController controller = new PaymentController(subscriptionCommandService, "price_test_123");

        assertThatThrownBy(controller::createCheckoutSession)
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });

        verifyNoInteractions(subscriptionCommandService);
    }
}