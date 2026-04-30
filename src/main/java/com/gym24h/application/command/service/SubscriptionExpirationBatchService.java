package com.gym24h.application.command.service;

import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.subscription.SubscriptionStatus;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.infrastructure.persistence.repository.AuditLogJdbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class SubscriptionExpirationBatchService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpirationBatchService.class);
    private static final String ACTION = "SYSTEM_EXPIRATION";
    private static final String RESULT = "SUCCESS";
    private static final String REASON = "Subscription expired by batch";

    private final SubscriptionRepository subscriptionRepository;
    private final AuditLogJdbcRepository auditLogJdbcRepository;
    private final Clock clock;

    public SubscriptionExpirationBatchService(
            SubscriptionRepository subscriptionRepository,
            AuditLogJdbcRepository auditLogJdbcRepository,
            Clock clock
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.auditLogJdbcRepository = auditLogJdbcRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void scheduledExpiration() {
        Instant now = Instant.now(clock);
        log.info("subscription expiration batch started now={}", now);
        try {
            int expiredCount = expireSubscriptions(now);
            log.info("subscription expiration batch expiredCount={}", expiredCount);
        } catch (Exception exception) {
            log.error("subscription expiration batch failed now={}", now, exception);
        }
        log.info("subscription expiration batch finished now={}", now);
    }

    @Transactional
    public int expireSubscriptions(Instant now) {
        List<Subscription> expiredCandidates = subscriptionRepository.findExpiredActiveSubscriptions(now);
        int expiredCount = 0;

        for (Subscription subscription : expiredCandidates) {
            subscription.expireIfNeeded(now, 0L);
            if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
                Subscription savedSubscription = subscriptionRepository.save(subscription);
                auditLogJdbcRepository.save(
                        savedSubscription.getUserId().value(),
                        ACTION,
                        RESULT,
                        REASON,
                        null
                );
                expiredCount++;
            }
        }

        return expiredCount;
    }
}