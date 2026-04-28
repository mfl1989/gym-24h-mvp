package com.gym24h.application.command.service;

import com.gym24h.domain.model.subscription.BillingCycle;
import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.user.User;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.domain.repository.UserRepository;
import com.gym24h.infrastructure.security.JwtTokenService;
import com.gym24h.presentation.response.AuthTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DebugLoginService {

    private static final Logger log = LoggerFactory.getLogger(DebugLoginService.class);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final JwtTokenService jwtTokenService;

    public DebugLoginService(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            JwtTokenService jwtTokenService
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public AuthTokenResponse loginOrProvision(UUID userId) {
        User user = ensureUserExists(userId);
        ensureActiveSubscriptionExists(user.getId(), Instant.now());
        return jwtTokenService.issueAuthenticationToken(userId);
    }

    @Transactional
    public AuthTokenResponse loginWithoutSubscription(UUID userId) {
        ensureUserExists(userId);
        return jwtTokenService.issueAuthenticationToken(userId);
    }

    private User ensureUserExists(UUID userIdValue) {
        UserId userId = new UserId(userIdValue);
        var existingUser = userRepository.findById(userId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        Instant now = Instant.now();
        User user = User.builder()
                .id(userId)
                .lineUserId("dev-login-" + userIdValue)
                .displayName("Dev User")
                .phoneNumber(null)
                .membershipStatus("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .version(0)
                .deleted(false)
                .subChar1(null)
                .subChar2(null)
                .subChar3(null)
                .subChar4(null)
                .subChar5(null)
                .subChar6(null)
                .subChar7(null)
                .subChar8(null)
                .subChar9(null)
                .subChar10(null)
                .build();

        try {
            return userRepository.save(user);
        } catch (DataAccessException exception) {
            exception.printStackTrace();
            log.error(
                    "Failed to provision debug login user. userId={}, lineUserId={}, membershipStatus={}",
                    userIdValue,
                    user.getLineUserId(),
                    user.getMembershipStatus(),
                    exception
            );
            throw exception;
        }
    }

    private Subscription ensureActiveSubscriptionExists(UserId userId, Instant now) {
        var existingSubscription = subscriptionRepository.findLatestByUserId(userId);
        if (existingSubscription.isPresent()) {
            return existingSubscription.get();
        }

        Subscription subscription = Subscription.create(userId, BillingCycle.THIRTY_DAYS, now)
                .toBuilder()
                .stripeSubscriptionId("dev-sub-" + userId.value())
                .build();

        try {
            return subscriptionRepository.save(subscription);
        } catch (DataAccessException exception) {
            exception.printStackTrace();
            log.error(
                    "Failed to provision debug subscription. userId={}, subscriptionStatus={}, billingCycle={}",
                    userId.value(),
                    subscription.getStatus(),
                    subscription.getBillingCycle(),
                    exception
            );
            throw exception;
        }
    }
}