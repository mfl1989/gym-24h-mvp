package com.gym24h.domain.model.invoice;

import com.gym24h.domain.model.subscription.SubscriptionId;
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
public class Invoice {

    private final InvoiceId id;
    private final SubscriptionId subscriptionId;
    private final UserId userId;
    private final String stripeInvoiceId;
    private final String stripeEventId;
    private final int amountPaid;
    private final String currency;
    private final InvoiceStatus status;
    private final String invoiceUrl;
    private final Instant billedAt;
    private final Instant paidAt;
    private final Instant dueAt;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final int version;
    private final boolean deleted;

    @Builder(toBuilder = true)
    public Invoice(
            InvoiceId id,
            SubscriptionId subscriptionId,
            UserId userId,
            String stripeInvoiceId,
            String stripeEventId,
            int amountPaid,
            String currency,
            InvoiceStatus status,
            String invoiceUrl,
            Instant billedAt,
            Instant paidAt,
            Instant dueAt,
            Instant createdAt,
            Instant updatedAt,
            int version,
            boolean deleted
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.subscriptionId = Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.stripeInvoiceId = Objects.requireNonNull(stripeInvoiceId, "stripeInvoiceId must not be null");
        this.stripeEventId = Objects.requireNonNull(stripeEventId, "stripeEventId must not be null");
        this.amountPaid = amountPaid;
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.invoiceUrl = invoiceUrl;
        this.billedAt = Objects.requireNonNull(billedAt, "billedAt must not be null");
        this.paidAt = paidAt;
        this.dueAt = dueAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.version = version;
        this.deleted = deleted;
    }
}
