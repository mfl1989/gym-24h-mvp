package com.gym24h.domain.model.subscription;

import com.gym24h.domain.model.user.UserId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class Subscription {

    private final SubscriptionId id;
    private final UserId userId;
    private final String planCode;
    private final BillingCycle billingCycle;
    private SubscriptionStatus status;
    private final String stripeCustomerId;
    private final String stripeSubscriptionId;
    private final Instant startedAt;
    private final Instant currentPeriodStartAt;
    private Instant currentPeriodEndAt;
    private Instant canceledAt;
    private Instant cancellationRequestedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;
    private boolean deleted;
    private final String subChar1;
    private final String subChar2;
    private final String subChar3;
    private final String subChar4;
    private final String subChar5;
    private final String subChar6;
    private final String subChar7;
    private final String subChar8;
    private final String subChar9;
    private final String subChar10;

    @Builder(toBuilder = true)
    public Subscription(
            SubscriptionId id,
            UserId userId,
            String planCode,
            BillingCycle billingCycle,
            SubscriptionStatus status,
            String stripeCustomerId,
            String stripeSubscriptionId,
            Instant startedAt,
            Instant currentPeriodStartAt,
            Instant currentPeriodEndAt,
            Instant canceledAt,
            Instant cancellationRequestedAt,
            Instant createdAt,
            Instant updatedAt,
            int version,
            boolean deleted,
            String subChar1,
            String subChar2,
            String subChar3,
            String subChar4,
            String subChar5,
            String subChar6,
            String subChar7,
            String subChar8,
            String subChar9,
            String subChar10
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.planCode = Objects.requireNonNull(planCode, "planCode must not be null");
        this.billingCycle = Objects.requireNonNull(billingCycle, "billingCycle must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.stripeCustomerId = stripeCustomerId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        this.currentPeriodStartAt = Objects.requireNonNull(currentPeriodStartAt, "currentPeriodStartAt must not be null");
        this.currentPeriodEndAt = Objects.requireNonNull(currentPeriodEndAt, "currentPeriodEndAt must not be null");
        this.canceledAt = canceledAt;
        this.cancellationRequestedAt = cancellationRequestedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.version = version;
        this.deleted = deleted;
        this.subChar1 = subChar1;
        this.subChar2 = subChar2;
        this.subChar3 = subChar3;
        this.subChar4 = subChar4;
        this.subChar5 = subChar5;
        this.subChar6 = subChar6;
        this.subChar7 = subChar7;
        this.subChar8 = subChar8;
        this.subChar9 = subChar9;
        this.subChar10 = subChar10;
    }

    public static Subscription create(UserId userId, BillingCycle billingCycle, Instant validFrom) {
        Instant startsAt = Objects.requireNonNull(validFrom, "validFrom must not be null");
        return Subscription.builder()
                .id(SubscriptionId.newId())
                .userId(userId)
                .planCode("STANDARD")
                .billingCycle(billingCycle)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(startsAt)
                .currentPeriodStartAt(startsAt)
                .currentPeriodEndAt(startsAt.plus(billingCycle.duration()))
                .createdAt(startsAt)
                .updatedAt(startsAt)
                .version(0)
                .deleted(false)
                .build();
    }

    public Instant getValidFrom() {
        return currentPeriodStartAt;
    }

    public Instant getValidUntil() {
        return currentPeriodEndAt;
    }

    public void markArrears() {
        requireStatus(SubscriptionStatus.ACTIVE, "Only ACTIVE subscriptions can move to ARREARS");
        this.status = SubscriptionStatus.ARREARS;
        touch();
    }

    public void markActive() {
        requireStatus(SubscriptionStatus.ARREARS, "Only ARREARS subscriptions can move to ACTIVE");
        this.status = SubscriptionStatus.ACTIVE;
        touch();
    }

    public void requestCancellation(Instant requestedAt) {
        requireStatus(SubscriptionStatus.ACTIVE, "Only ACTIVE subscriptions can move to CANCELED");
        this.status = SubscriptionStatus.CANCELED;
        this.cancellationRequestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        this.canceledAt = requestedAt;
        touch();
    }

    public void expireIfNeeded(Instant now, long bufferSeconds) {
        Instant threshold = currentPeriodEndAt.plusSeconds(bufferSeconds);
        if (!now.isBefore(threshold)) {
            this.status = SubscriptionStatus.EXPIRED;
            touch();
        }
    }

    public boolean canEnter(Instant now, long bufferSeconds) {
        expireIfNeeded(now, bufferSeconds);
        return status == SubscriptionStatus.ACTIVE && !deleted && now.isBefore(currentPeriodEndAt.plusSeconds(bufferSeconds));
    }

    public void markDeleted() {
        this.deleted = true;
        touch();
    }

    public void syncVersion(int version) {
        this.version = version;
    }

    private void requireStatus(SubscriptionStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message + ": current=" + status);
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
