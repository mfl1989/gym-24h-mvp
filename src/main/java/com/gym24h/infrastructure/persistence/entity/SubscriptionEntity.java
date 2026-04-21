package com.gym24h.infrastructure.persistence.entity;

import com.gym24h.domain.model.subscription.BillingCycle;
import com.gym24h.domain.model.subscription.SubscriptionStatus;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "plan_code", nullable = false)
    private String planCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "current_period_start_at", nullable = false)
    private Instant currentPeriodStartAt;

    @Column(name = "current_period_end_at", nullable = false)
    private Instant currentPeriodEndAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "cancellation_requested_at")
    private Instant cancellationRequestedAt;

    @Version
    private int version;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sub_char1")
    private String subChar1;

    @Column(name = "sub_char2")
    private String subChar2;

    @Column(name = "sub_char3")
    private String subChar3;

    @Column(name = "sub_char4")
    private String subChar4;

    @Column(name = "sub_char5")
    private String subChar5;

    @Column(name = "sub_char6")
    private String subChar6;

    @Column(name = "sub_char7")
    private String subChar7;

    @Column(name = "sub_char8")
    private String subChar8;

    @Column(name = "sub_char9")
    private String subChar9;

    @Column(name = "sub_char10")
    private String subChar10;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

