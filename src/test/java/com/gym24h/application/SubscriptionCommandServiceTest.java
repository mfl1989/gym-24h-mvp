package com.gym24h.application;

import com.gym24h.application.command.dto.CreateSubscriptionCommand;
import com.gym24h.application.command.service.SubscriptionCommandService;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.domain.model.subscription.BillingCycle;
import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.service.SubscriptionDomainService;
import com.gym24h.domain.repository.InvoiceRepository;
import com.gym24h.domain.model.user.User;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.domain.repository.UserRepository;
import com.gym24h.infrastructure.external.stripe.StripeClient;
import com.gym24h.infrastructure.persistence.repository.AuditLogJdbcRepository;
import com.gym24h.infrastructure.persistence.repository.ProcessedWebhookEventJdbcRepository;
import com.stripe.exception.ApiConnectionException;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private SubscriptionDomainService subscriptionDomainService;

    @Mock
    private AuditLogJdbcRepository auditLogJdbcRepository;

    @Mock
    private ProcessedWebhookEventJdbcRepository processedWebhookEventJdbcRepository;

    @Mock
    private StripeClient stripeClient;

    @Test
    void shouldCreateSubscription() {
        SubscriptionCommandService subscriptionCommandService = createService();
        UUID userUuid = UUID.randomUUID();
        User user = new User(new UserId(userUuid), "line-user-1", true, 0L, false);
        Instant startsAt = Instant.parse("2026-01-01T00:00:00Z");

        when(userRepository.findById(new UserId(userUuid))).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = subscriptionCommandService.create(new CreateSubscriptionCommand(userUuid, BillingCycle.THIRTY_DAYS, startsAt));

        assertEquals(userUuid, result.userId());
        assertEquals(BillingCycle.THIRTY_DAYS, result.billingCycle());
    }

    @Test
    void shouldWrapStripeExceptionAndBuildCheckoutSessionParams() throws Exception {
        SubscriptionCommandService subscriptionCommandService = createService();
        UUID userUuid = UUID.randomUUID();
        User user = new User(new UserId(userUuid), "line-user-1", true, 0L, false);

        when(userRepository.findById(new UserId(userUuid))).thenReturn(Optional.of(user));
        when(subscriptionRepository.findActiveOrArrearsByUserId(new UserId(userUuid))).thenReturn(Optional.empty());
        when(stripeClient.createCheckoutSession(any(SessionCreateParams.class)))
                .thenThrow(new ApiConnectionException("stripe unavailable"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> subscriptionCommandService.createCheckoutSession(userUuid, "price_basic")
        );

        assertEquals(ErrorCodes.STRIPE_API_ERROR, exception.getCode());
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());

        ArgumentCaptor<SessionCreateParams> paramsCaptor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(stripeClient).createCheckoutSession(paramsCaptor.capture());
        SessionCreateParams params = paramsCaptor.getValue();

        assertEquals(SessionCreateParams.Mode.SUBSCRIPTION, params.getMode());
        assertEquals("http://localhost:3000/success?session_id={CHECKOUT_SESSION_ID}", params.getSuccessUrl());
        assertEquals("http://localhost:3000/cancel", params.getCancelUrl());
        assertEquals(userUuid.toString(), params.getMetadata().get("userId"));
        assertEquals(1, params.getLineItems().size());
        assertEquals("price_basic", params.getLineItems().getFirst().getPrice());
        assertEquals(1L, params.getLineItems().getFirst().getQuantity());
    }

    @Test
    void shouldRejectCancellationOutsideFirstFifteenDays() throws Exception {
        SubscriptionCommandService subscriptionCommandService = createService();
        UUID userUuid = UUID.randomUUID();
        Instant now = Instant.now();
        User user = new User(new UserId(userUuid), "line-user-1", true, 0L, false);
        Subscription subscription = Subscription.create(new UserId(userUuid), BillingCycle.THIRTY_DAYS, now.minusSeconds(20L * 24L * 60L * 60L))
                .toBuilder()
                .stripeSubscriptionId("sub_cancel_001")
                .build();

        when(subscriptionRepository.findLatestByUserId(new UserId(userUuid))).thenReturn(Optional.of(subscription));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> subscriptionCommandService.cancelSubscriptionAtPeriodEnd(userUuid)
        );

        assertEquals(ErrorCodes.FORBIDDEN, exception.getCode());
        verify(stripeClient, never()).scheduleCancellationAtPeriodEnd(any());
    }

    private SubscriptionCommandService createService() {
        return new SubscriptionCommandService(
                userRepository,
                subscriptionRepository,
                invoiceRepository,
                subscriptionDomainService,
                auditLogJdbcRepository,
                processedWebhookEventJdbcRepository,
                stripeClient,
                "http://localhost:3000/success?session_id={CHECKOUT_SESSION_ID}",
                "http://localhost:3000/cancel"
        );
    }
}
