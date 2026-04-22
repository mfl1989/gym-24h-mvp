package com.gym24h.application.command.service;

import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.domain.model.invoice.InvoiceStatus;
import com.gym24h.domain.repository.InvoiceRepository;
import com.gym24h.infrastructure.external.stripe.StripeClient;
import com.gym24h.infrastructure.persistence.repository.AuditLogJdbcRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Stripe 請求データとローカル台帳を付き合わせる補償サービス。
 *
 * Webhook は到達保証ではないため、金融系では「届かなかった前提」で再照合の経路を持つ必要がある。
 * このサービスは Stripe の請求一覧を正としてローカル DB を補正し、未反映または不一致の請求だけを再適用する。
 */
@Service
public class SubscriptionReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionReconciliationService.class);
    private static final String RECONCILIATION_ACTION = "SYSTEM_RECONCILIATION";

    private final StripeClient stripeClient;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionCommandService subscriptionCommandService;
    private final AuditLogJdbcRepository auditLogJdbcRepository;
    private final Clock clock;
    private final boolean reconciliationEnabled;
    private final long lookbackHours;

    public SubscriptionReconciliationService(
            StripeClient stripeClient,
            InvoiceRepository invoiceRepository,
            SubscriptionCommandService subscriptionCommandService,
            AuditLogJdbcRepository auditLogJdbcRepository,
            Clock clock,
            @Value("${stripe.reconciliation.enabled:true}") boolean reconciliationEnabled,
            @Value("${stripe.reconciliation.lookback-hours:24}") long lookbackHours
    ) {
        this.stripeClient = stripeClient;
        this.invoiceRepository = invoiceRepository;
        this.subscriptionCommandService = subscriptionCommandService;
        this.auditLogJdbcRepository = auditLogJdbcRepository;
        this.clock = clock;
        this.reconciliationEnabled = reconciliationEnabled;
        this.lookbackHours = lookbackHours;
    }

    @Transactional
    public int reconcileWithStripe(Instant lookbackPeriod) {
        try {
            List<Invoice> stripeInvoices = stripeClient.listInvoicesSince(lookbackPeriod);
            int correctedCount = 0;
            for (Invoice stripeInvoice : stripeInvoices) {
                if (requiresCorrection(stripeInvoice)) {
                    try {
                        subscriptionCommandService.reconcileInvoiceFromStripe(
                                stripeInvoice,
                                buildReconciliationSourceId(stripeInvoice)
                        );
                        correctedCount++;
                        auditLogJdbcRepository.save(
                                resolveAuditUserId(stripeInvoice),
                                RECONCILIATION_ACTION,
                                "SUCCESS",
                                buildSuccessReason(stripeInvoice),
                                null
                        );
                    } catch (BusinessException exception) {
                        auditLogJdbcRepository.save(
                                resolveAuditUserId(stripeInvoice),
                                RECONCILIATION_ACTION,
                                "FAILURE",
                                buildFailureReason(stripeInvoice, exception.getMessage()),
                                null
                        );
                        log.warn("subscription reconciliation skipped invoiceId={} reason={}", stripeInvoice.getId(), exception.getMessage());
                    }
                }
            }
            return correctedCount;
        } catch (StripeException exception) {
            throw new BusinessException(
                    ErrorCodes.STRIPE_API_ERROR,
                    "Failed to reconcile invoices with Stripe",
                    org.springframework.http.HttpStatus.BAD_GATEWAY
            );
        }
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void scheduledReconciliation() {
        if (!reconciliationEnabled) {
            log.info("subscription reconciliation skipped because stripe.reconciliation.enabled=false");
            return;
        }

        Instant lookbackPeriod = Instant.now(clock).minus(Duration.ofHours(lookbackHours));
        log.info("subscription reconciliation started lookbackPeriod={} lookbackHours={}", lookbackPeriod, lookbackHours);
        try {
            int correctedCount = reconcileWithStripe(lookbackPeriod);
            log.info("subscription reconciliation correctedCount={}", correctedCount);
        } catch (Exception exception) {
            log.error("subscription reconciliation failed", exception);
        }
        log.info("subscription reconciliation finished lookbackPeriod={}", lookbackPeriod);
    }

    private boolean requiresCorrection(Invoice stripeInvoice) {
        InvoiceStatus remoteStatus = toInvoiceStatus(stripeInvoice);
        return invoiceRepository.findByStripeInvoiceId(stripeInvoice.getId())
                .map(localInvoice -> localInvoice.getStatus() != remoteStatus)
                .orElse(true);
    }

    private InvoiceStatus toInvoiceStatus(Invoice stripeInvoice) {
        if (Boolean.TRUE.equals(stripeInvoice.getPaid()) || firstNonNullLong(stripeInvoice.getAmountPaid(), 0L) > 0L) {
            return InvoiceStatus.PAID;
        }
        return InvoiceStatus.FAILED;
    }

    private String buildReconciliationSourceId(Invoice stripeInvoice) {
        return "reconcile:" + stripeInvoice.getId() + ":" + toInvoiceStatus(stripeInvoice).name();
    }

    private UUID resolveAuditUserId(Invoice stripeInvoice) {
        return invoiceRepository.findByStripeInvoiceId(stripeInvoice.getId())
                .map(localInvoice -> localInvoice.getUserId().value())
                .orElseGet(() -> {
                    String userId = stripeInvoice.getMetadata() == null ? null : stripeInvoice.getMetadata().get("userId");
                    return userId == null || userId.isBlank() ? null : UUID.fromString(userId);
                });
    }

    private String buildSuccessReason(Invoice stripeInvoice) {
        return "Reconciled Stripe invoice id=" + stripeInvoice.getId() + " status=" + toInvoiceStatus(stripeInvoice).name();
    }

    private String buildFailureReason(Invoice stripeInvoice, String detail) {
        return "Failed to reconcile Stripe invoice id=" + stripeInvoice.getId() + " reason=" + detail;
    }

    private long firstNonNullLong(Long value, long fallback) {
        return value == null ? fallback : value;
    }
}