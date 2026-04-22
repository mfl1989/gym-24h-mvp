package com.gym24h.application.command.service;

import com.gym24h.application.command.dto.CreateSubscriptionCommand;
import com.gym24h.application.query.dto.SubscriptionView;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.domain.model.invoice.InvoiceId;
import com.gym24h.domain.model.invoice.InvoiceStatus;
import com.gym24h.domain.model.subscription.BillingCycle;
import com.gym24h.domain.model.subscription.Subscription;
import com.gym24h.domain.model.subscription.SubscriptionId;
import com.gym24h.domain.model.subscription.SubscriptionStatus;
import com.gym24h.domain.repository.InvoiceRepository;
import com.gym24h.domain.model.user.User;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.domain.repository.UserRepository;
import com.gym24h.domain.service.SubscriptionDomainService;
import com.gym24h.infrastructure.external.stripe.StripeClient;
import com.gym24h.infrastructure.persistence.repository.AuditLogJdbcRepository;
import com.gym24h.infrastructure.persistence.repository.ProcessedWebhookEventJdbcRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Stripe 課金イベントと会員購読状態を整合させる中核アプリケーションサービス。
 *
 * Checkout 作成、Webhook 処理、対帳バッチからの補正適用を一つの規則に寄せ、
 * 「Stripe を支払い状態の SSOT とする」という業務原則を破らないことを責務とする。
 * 決済状態は必ず Stripe 由来のイベントから反映し、フロント経由の自己申告では更新しない。
 */
@Service
public class SubscriptionCommandService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionCommandService.class);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionDomainService subscriptionDomainService;
    private final AuditLogJdbcRepository auditLogJdbcRepository;
    private final ProcessedWebhookEventJdbcRepository processedWebhookEventJdbcRepository;
    private final StripeClient stripeClient;
    private final String checkoutSuccessUrl;
    private final String checkoutCancelUrl;

    public SubscriptionCommandService(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            InvoiceRepository invoiceRepository,
            SubscriptionDomainService subscriptionDomainService,
            AuditLogJdbcRepository auditLogJdbcRepository,
            ProcessedWebhookEventJdbcRepository processedWebhookEventJdbcRepository,
            StripeClient stripeClient,
            @Value("${stripe.checkout.success-url}") String checkoutSuccessUrl,
            @Value("${stripe.checkout.cancel-url}") String checkoutCancelUrl
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.subscriptionDomainService = subscriptionDomainService;
        this.auditLogJdbcRepository = auditLogJdbcRepository;
        this.processedWebhookEventJdbcRepository = processedWebhookEventJdbcRepository;
        this.stripeClient = stripeClient;
        this.checkoutSuccessUrl = checkoutSuccessUrl;
        this.checkoutCancelUrl = checkoutCancelUrl;
    }

    @Transactional
    public SubscriptionView create(CreateSubscriptionCommand command) {
        UserId userId = new UserId(command.userId());
        requireUser(userId);

        Subscription subscription = Subscription.create(userId, command.billingCycle(), command.startsAt());
        Subscription saved = subscriptionRepository.save(subscription);
        return new SubscriptionView(
                saved.getId().value(),
                saved.getUserId().value(),
                saved.getBillingCycle(),
                saved.getStatus(),
                saved.getValidFrom(),
                saved.getValidUntil(),
                saved.getCancellationRequestedAt()
        );
    }

    @Transactional(readOnly = true)
    public String createCheckoutSession(java.util.UUID userIdValue, String priceId) {
        UserId userId = new UserId(userIdValue);
        requireUser(userId);

        if (priceId == null || priceId.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR,
                    "priceId must not be blank",
                    HttpStatus.BAD_REQUEST
            );
        }

        subscriptionRepository.findActiveOrArrearsByUserId(userId)
                .ifPresent(subscription -> {
                    throw new BusinessException(
                            ErrorCodes.BUSINESS_RULE_VIOLATION,
                            "An ACTIVE or ARREARS subscription already exists",
                            HttpStatus.CONFLICT
                    );
                });

        // 初回課金完了前でも subscriptionId を先に採番して metadata に埋めることで、
        // Webhook 到着時に「どの購読を作る/補正するか」を決済結果だけで確定できる。
        SubscriptionId pendingSubscriptionId = SubscriptionId.newId();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(checkoutSuccessUrl)
                .setCancelUrl(checkoutCancelUrl)
                .putMetadata("userId", userId.value().toString())
                .putMetadata("subscriptionId", pendingSubscriptionId.value().toString())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPrice(priceId)
                                .build()
                )
                .build();

        try {
            Session session = stripeClient.createCheckoutSession(params);
            log.info("created stripe checkout session sessionId={} userId={} subscriptionId={}",
                        session.getId(), userId.value(), pendingSubscriptionId.value());
            return session.getUrl();
        } catch (StripeException exception) {
            throw new BusinessException(
                        ErrorCodes.STRIPE_API_ERROR,
                        "Failed to create Stripe checkout session",
                        HttpStatus.BAD_GATEWAY
            );
        }
    }

    @Transactional
    public void cancelSubscriptionAtPeriodEnd(UUID userIdValue) {
        UserId userId = new UserId(userIdValue);
        Subscription subscription = subscriptionRepository.findLatestByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "Subscription not found",
                        HttpStatus.NOT_FOUND
                ));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCodes.FORBIDDEN,
                    "Only ACTIVE subscriptions can request cancellation",
                    HttpStatus.FORBIDDEN
            );
        }

        Instant now = Instant.now();
        Instant cancellationWindowEnd = subscription.getCurrentPeriodStartAt().plusSeconds(15L * 24L * 60L * 60L);
        if (now.isAfter(cancellationWindowEnd)) {
            throw new BusinessException(
                    ErrorCodes.FORBIDDEN,
                    "已过本期前 15 天退会窗口，请下个周期再试，下期费用将正常扣除",
                    HttpStatus.FORBIDDEN
            );
        }

        if (subscription.getCancellationRequestedAt() != null) {
            return;
        }

        if (subscription.getStripeSubscriptionId() == null || subscription.getStripeSubscriptionId().isBlank()) {
            throw new BusinessException(
                    ErrorCodes.RESOURCE_NOT_FOUND,
                    "Stripe subscription not found",
                    HttpStatus.NOT_FOUND
            );
        }

        try {
            stripeClient.scheduleCancellationAtPeriodEnd(subscription.getStripeSubscriptionId());
        } catch (Exception e) {
            log.warn("Stripe 调用失败，跳过网关请求继续更新本地状态: {}", e.getMessage());
        }

        Subscription updated = subscriptionDomainService.requestCancellation(subscription, now);
        subscriptionRepository.save(updated);
    }

    @Transactional
    public void revokeSubscriptionCancellation(UUID userIdValue) {
        UserId userId = new UserId(userIdValue);
        Subscription subscription = subscriptionRepository.findLatestByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "Subscription not found",
                        HttpStatus.NOT_FOUND
                ));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCodes.FORBIDDEN,
                    "Only ACTIVE subscriptions can revoke cancellation",
                    HttpStatus.FORBIDDEN
            );
        }

        if (subscription.getCancellationRequestedAt() == null) {
            throw new BusinessException(
                    ErrorCodes.FORBIDDEN,
                    "Cancellation has not been requested",
                    HttpStatus.FORBIDDEN
            );
        }

        if (subscription.getStripeSubscriptionId() == null || subscription.getStripeSubscriptionId().isBlank()) {
            throw new BusinessException(
                    ErrorCodes.RESOURCE_NOT_FOUND,
                    "Stripe subscription not found",
                    HttpStatus.NOT_FOUND
            );
        }

        try {
            stripeClient.revokeCancellationAtPeriodEnd(subscription.getStripeSubscriptionId());
        } catch (Exception e) {
            log.warn("Stripe 调用失败，跳过网关请求继续更新本地状态: {}", e.getMessage());
        }

        Subscription updated = subscriptionDomainService.revokeCancellation(subscription);
        subscriptionRepository.save(updated);
    }

    @Transactional
    public void forceTerminateSubscription(UUID userIdValue, String reason) {
    if (reason == null || reason.isBlank()) {
        throw new BusinessException(
            ErrorCodes.VALIDATION_ERROR,
            "reason must not be blank",
            HttpStatus.BAD_REQUEST
        );
    }

    UserId userId = new UserId(userIdValue);
    Subscription subscription = subscriptionRepository.findLatestByUserId(userId)
        .orElseThrow(() -> new BusinessException(
            ErrorCodes.RESOURCE_NOT_FOUND,
            "Subscription not found",
            HttpStatus.NOT_FOUND
        ));

    Subscription updated = subscriptionDomainService.forceTerminate(subscription, Instant.now());
    subscriptionRepository.save(updated);
    auditLogJdbcRepository.save(
        userIdValue,
        "FORCE_TERMINATED",
        "SUCCESS",
        reason,
        null
    );
    }

    @Async
    @Transactional
    public void handleVerifiedWebhookEventAsync(Event event) {
        try {
            handleWebhookEvent(event);
        } catch (Exception exception) {
            log.error("stripe webhook handling failed", exception);
        }
    }

    public Event verifyWebhookSignature(String payload, String sigHeader) throws SignatureVerificationException {
        return stripeClient.verifyWebhookSignature(payload, sigHeader);
    }

    @Transactional
    public void reconcileInvoiceFromStripe(Invoice stripeInvoice, String sourceId) {
        InvoiceStatus invoiceStatus = resolveInvoiceStatus(stripeInvoice);
        if (invoiceStatus == InvoiceStatus.PAID) {
            handleInvoicePaymentSucceeded(stripeInvoice, sourceId);
            return;
        }
        handleInvoicePaymentFailed(stripeInvoice, sourceId);
    }

    @Transactional
    public void handleWebhookEvent(Event event) {
        if (event.getId() == null || event.getId().isBlank()) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR,
                    "stripe event_id is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Webhook は再送が前提のため、event_id 単位で一意化しないと購読延長や請求保存が二重反映される。
        // 金流では「同じイベントは何回届いても結果が一回と同じ」であることが必須。
        boolean acquired = processedWebhookEventJdbcRepository.tryAcquire(event.getId(), event.getType());
        if (!acquired) {
            log.info("stripe webhook already processed eventId={}", event.getId());
            return;
        }

        if (!"checkout.session.completed".equals(event.getType())) {
            if ("invoice.payment_succeeded".equals(event.getType())) {
                handleInvoicePaymentSucceeded(requireInvoice(event), event.getId());
                return;
            }
            if ("invoice.payment_failed".equals(event.getType())) {
                handleInvoicePaymentFailed(requireInvoice(event), event.getId());
                return;
            }
            log.info("stripe webhook ignored eventId={} type={}", event.getId(), event.getType());
            return;
        }

        Session session = requireCheckoutSession(event);
        UUID userIdValue = extractUserId(session);
        UserId userId = new UserId(userIdValue);
        SubscriptionId subscriptionId = extractSubscriptionId(session);

        subscriptionRepository.findById(subscriptionId)
                .map(subscription -> activateAndAttachStripeReferences(subscription, session))
                .orElseGet(() -> createActivatedSubscription(subscriptionId, userId, session));
    }

    private User requireUser(UserId userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "User not found",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Session requireCheckoutSession(Event event) {
        StripeObject stripeObject;
        try {
            stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException exception) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR,
                    "Invalid Stripe checkout session payload",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (stripeObject == null) {
            throw new BusinessException(
                ErrorCodes.VALIDATION_ERROR,
                "Stripe event data.object is required",
                HttpStatus.BAD_REQUEST
            );
        }
        if (!(stripeObject instanceof Session session)) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR,
                    "Stripe event data.object must be a checkout session",
                    HttpStatus.BAD_REQUEST
            );
        }
        return session;
    }

    private UUID extractUserId(Session session) {
        String userId = session.getMetadata() == null ? null : session.getMetadata().get("userId");
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR,
                    "Stripe metadata.userId is required",
                    HttpStatus.BAD_REQUEST
            );
        }
        return UUID.fromString(userId);
    }

    private SubscriptionId extractSubscriptionId(Session session) {
        String subscriptionId = session.getMetadata() == null ? null : session.getMetadata().get("subscriptionId");
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR,
                    "Stripe metadata.subscriptionId is required",
                    HttpStatus.BAD_REQUEST
            );
        }
        return new SubscriptionId(UUID.fromString(subscriptionId));
    }

    private Subscription activateAndAttachStripeReferences(Subscription subscription, Session session) {
        if (subscription.getStatus() == SubscriptionStatus.ARREARS) {
            subscriptionDomainService.recoverFromArrears(subscription);
        }
        return subscriptionRepository.save(subscription.toBuilder()
                .stripeCustomerId(session.getCustomer())
                .stripeSubscriptionId(session.getSubscription())
                .build());
    }

    private void handleInvoicePaymentSucceeded(Invoice invoice, String sourceId) {
        Subscription subscription = locateSubscriptionForInvoice(invoice);

        // 継続課金成功時は Stripe を正として有効期限を延長する。
        // Webhook/対帳のどちらから来ても同じ補正結果になるよう sourceId だけ差し替えて再利用する。
        Subscription updated = subscriptionDomainService.handleRecurringPaymentSuccess(subscription, Instant.now());
        Subscription savedSubscription = subscriptionRepository.save(updated.toBuilder()
                .stripeCustomerId(firstNonBlank(invoice.getCustomer(), subscription.getStripeCustomerId()))
                .stripeSubscriptionId(firstNonBlank(invoice.getSubscription(), subscription.getStripeSubscriptionId()))
                .build());
        upsertInvoiceRecord(sourceId, invoice, savedSubscription, InvoiceStatus.PAID);
    }

    private void handleInvoicePaymentFailed(Invoice invoice, String sourceId) {
        Subscription subscription = locateSubscriptionForInvoice(invoice);
        Subscription updated = subscriptionDomainService.handleRecurringPaymentFailure(subscription);
        Subscription savedSubscription = subscriptionRepository.save(updated.toBuilder()
                .stripeCustomerId(firstNonBlank(invoice.getCustomer(), subscription.getStripeCustomerId()))
                .stripeSubscriptionId(firstNonBlank(invoice.getSubscription(), subscription.getStripeSubscriptionId()))
                .build());
        upsertInvoiceRecord(sourceId, invoice, savedSubscription, InvoiceStatus.FAILED);

        // 欠費は後続の入館拒否や督促判断に直結するため、監査ログに失敗理由を必ず残す。
        auditLogJdbcRepository.save(
                subscription.getUserId().value(),
                "SUBSCRIPTION_PAYMENT",
                "FAILURE",
                resolveInvoiceFailureReason(invoice),
                null
        );
    }

    private Subscription createActivatedSubscription(SubscriptionId subscriptionId, UserId userId, Session session) {
        Subscription subscription = Subscription.create(userId, BillingCycle.THIRTY_DAYS, Instant.now())
                .toBuilder()
                .id(subscriptionId)
                .stripeCustomerId(session.getCustomer())
                .stripeSubscriptionId(session.getSubscription())
                .build();
        return subscriptionRepository.save(subscription);
    }

    private Invoice requireInvoice(Event event) {
        StripeObject stripeObject;
        try {
            stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException exception) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR,
                    "Invalid Stripe invoice payload",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (!(stripeObject instanceof Invoice invoice)) {
            throw new BusinessException(
                    ErrorCodes.VALIDATION_ERROR,
                    "Stripe event data.object must be an invoice",
                    HttpStatus.BAD_REQUEST
            );
        }
        return invoice;
    }

    private Subscription locateSubscriptionForInvoice(Invoice invoice) {
        String stripeSubscriptionId = invoice.getSubscription();

        // userId metadata は補助情報として扱い、第一候補は永続化済みの stripeSubscriptionId とする。
        // 継続課金では userId が欠落しても Stripe 契約 ID だけで照合できるようにしておく必要がある。
        if (stripeSubscriptionId != null && !stripeSubscriptionId.isBlank()) {
            return subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                    .orElseGet(() -> findSubscriptionByMetadataUserId(invoice));
        }
        return findSubscriptionByMetadataUserId(invoice);
    }

    private Subscription findSubscriptionByMetadataUserId(Invoice invoice) {
        String userId = invoice.getMetadata() == null ? null : invoice.getMetadata().get("userId");
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(
                    ErrorCodes.RESOURCE_NOT_FOUND,
                    "Subscription could not be resolved from Stripe invoice",
                    HttpStatus.NOT_FOUND
            );
        }
        return subscriptionRepository.findActiveOrArrearsByUserId(new UserId(UUID.fromString(userId)))
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "Subscription not found for Stripe invoice",
                        HttpStatus.NOT_FOUND
                ));
    }

    private String resolveInvoiceFailureReason(Invoice invoice) {
        String reason = invoice.getMetadata() == null ? null : invoice.getMetadata().get("failureReason");
        if (reason != null && !reason.isBlank()) {
            return reason;
        }
        String stripeSubscriptionId = invoice.getSubscription();
        if (stripeSubscriptionId != null && !stripeSubscriptionId.isBlank()) {
            return "Stripe reported payment failure for subscription=" + stripeSubscriptionId;
        }
        return "Stripe reported payment failure";
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    private void upsertInvoiceRecord(String sourceId, com.stripe.model.Invoice stripeInvoice, Subscription subscription, InvoiceStatus status) {
        Instant now = Instant.now();
        Instant billedAt = epochSecondsToInstant(stripeInvoice.getCreated(), now);
        Instant paidAt = status == InvoiceStatus.PAID ? epochSecondsToInstant(stripeInvoice.getStatusTransitions() == null ? null : stripeInvoice.getStatusTransitions().getPaidAt(), now) : null;
        Instant dueAt = epochSecondsToInstant(stripeInvoice.getDueDate(), null);
        int amountPaid = Math.toIntExact(firstNonNullLong(stripeInvoice.getAmountPaid(), 0L));
        String currency = firstNonBlank(stripeInvoice.getCurrency(), "usd");

        // 同一 invoice は失敗後に再回収されることがあるため、insert 固定ではなく upsert にする。
        // これにより「最新の Stripe 状態」をローカル請求台帳に寄せ続けられる。
        com.gym24h.domain.model.invoice.Invoice invoiceRecord = invoiceRepository.findByStripeInvoiceId(stripeInvoice.getId())
                .map(existing -> existing.toBuilder()
                        .subscriptionId(subscription.getId())
                        .userId(subscription.getUserId())
                .stripeEventId(sourceId)
                        .amountPaid(amountPaid)
                        .currency(currency)
                        .status(status)
                        .invoiceUrl(stripeInvoice.getHostedInvoiceUrl())
                        .billedAt(billedAt)
                        .paidAt(paidAt)
                        .dueAt(dueAt)
                        .updatedAt(now)
                        .build())
                .orElseGet(() -> com.gym24h.domain.model.invoice.Invoice.builder()
                        .id(InvoiceId.newId())
                        .subscriptionId(subscription.getId())
                        .userId(subscription.getUserId())
                        .stripeInvoiceId(stripeInvoice.getId())
                        .stripeEventId(sourceId)
                        .amountPaid(amountPaid)
                        .currency(currency)
                        .status(status)
                        .invoiceUrl(stripeInvoice.getHostedInvoiceUrl())
                        .billedAt(billedAt)
                        .paidAt(paidAt)
                        .dueAt(dueAt)
                        .createdAt(now)
                        .updatedAt(now)
                        .version(0)
                        .deleted(false)
                        .build());
        invoiceRepository.save(invoiceRecord);
    }

    private Instant epochSecondsToInstant(Long epochSeconds, Instant fallback) {
        if (epochSeconds == null) {
            return fallback;
        }
        return Instant.ofEpochSecond(epochSeconds);
    }

    private long firstNonNullLong(Long value, long fallback) {
        return value == null ? fallback : value;
    }

    private InvoiceStatus resolveInvoiceStatus(Invoice invoice) {
        if (Boolean.TRUE.equals(invoice.getPaid()) || firstNonNullLong(invoice.getAmountPaid(), 0L) > 0L) {
            return InvoiceStatus.PAID;
        }
        return InvoiceStatus.FAILED;
    }
}
