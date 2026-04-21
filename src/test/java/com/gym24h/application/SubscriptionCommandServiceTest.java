package com.gym24h.application;

import com.gym24h.application.command.dto.CreateSubscriptionCommand;
import com.gym24h.application.command.service.SubscriptionCommandService;
import com.gym24h.domain.model.subscription.BillingCycle;
import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.user.User;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionCommandService subscriptionCommandService;

    @Test
    void shouldCreateSubscription() {
        UUID userUuid = UUID.randomUUID();
        User user = new User(new UserId(userUuid), "line-user-1", true, 0L, false);
        Instant startsAt = Instant.parse("2026-01-01T00:00:00Z");

        when(userRepository.findById(new UserId(userUuid))).thenReturn(Optional.of(user));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = subscriptionCommandService.create(new CreateSubscriptionCommand(userUuid, BillingCycle.THIRTY_DAYS, startsAt));

        assertEquals(userUuid, result.userId());
        assertEquals(BillingCycle.THIRTY_DAYS, result.billingCycle());
    }
}
