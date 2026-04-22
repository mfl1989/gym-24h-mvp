package com.gym24h.infrastructure.persistence.entity;

import com.gym24h.domain.model.invoice.InvoiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "invoices")
public class InvoiceEntity {

    @Id
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "stripe_invoice_id", nullable = false)
    private String stripeInvoiceId;

    @Column(name = "stripe_event_id", nullable = false)
    private String stripeEventId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    @Column(name = "invoice_url")
    private String invoiceUrl;

    @Column(name = "billed_at", nullable = false)
    private Instant billedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Version
    private int version;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
