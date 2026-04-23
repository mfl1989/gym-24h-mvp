package com.gym24h.application.command.service;

import com.gym24h.application.command.dto.OpenDoorCommand;
import com.gym24h.application.outbound.DoorLockClient;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.common.exception.InfrastructureException;
import com.gym24h.common.logging.RequestIdFilter;
import com.gym24h.domain.service.GeneratedQrToken;
import com.gym24h.domain.model.subscription.SubscriptionId;
import com.gym24h.domain.model.subscription.SubscriptionStatus;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.domain.service.EntranceValidator;
import com.gym24h.domain.service.QrTokenGenerator;
import com.gym24h.infrastructure.cache.redis.IdempotencyService;
import com.gym24h.infrastructure.persistence.repository.AuditLogJdbcRepository;
import com.gym24h.infrastructure.security.JwtTokenService;
import com.gym24h.infrastructure.security.QrTokenClaims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 開錠要求を業務ルールに沿って編成するアプリケーションサービス。
 *
 * QR の署名検証、認証主体との突合、防重放、5 秒単位の冪等制御、監査ログ記録を一箇所に集約し、
 * 「開いたかどうか」より前に「誰の権限で、何度目の要求で、再送かどうか」を必ず確定させる責務を持つ。
 */
@Service
public class EntranceCommandService {

    private static final String OPEN_DOOR_ACTION = "OPEN_DOOR";

    private final SubscriptionRepository subscriptionRepository;
    private final EntranceValidator entranceValidator;
    private final QrTokenGenerator qrTokenGenerator;
    private final IdempotencyService idempotencyService;
    private final JwtTokenService jwtTokenService;
    private final AuditLogJdbcRepository auditLogJdbcRepository;
    private final DoorLockClient doorLockClient;

    public EntranceCommandService(
            SubscriptionRepository subscriptionRepository,
            EntranceValidator entranceValidator,
            QrTokenGenerator qrTokenGenerator,
            IdempotencyService idempotencyService,
            JwtTokenService jwtTokenService,
            AuditLogJdbcRepository auditLogJdbcRepository,
            DoorLockClient doorLockClient
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.entranceValidator = entranceValidator;
        this.qrTokenGenerator = qrTokenGenerator;
        this.idempotencyService = idempotencyService;
        this.jwtTokenService = jwtTokenService;
        this.auditLogJdbcRepository = auditLogJdbcRepository;
        this.doorLockClient = doorLockClient;
    }

    public GeneratedQrToken issueQrToken(UUID userId, UUID subscriptionId) {
        var subscription = subscriptionRepository.findById(new SubscriptionId(subscriptionId))
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "Subscription not found",
                        HttpStatus.NOT_FOUND
                ));

        if (!subscription.getUserId().value().equals(userId)) {
            throw new BusinessException(
                    ErrorCodes.FORBIDDEN,
                    "Subscription does not belong to the authenticated user",
                    HttpStatus.FORBIDDEN
            );
        }

        entranceValidator.validate(subscription, Instant.now());
        return qrTokenGenerator.generate(userId, subscriptionId);
    }

    public GeneratedQrToken issueCurrentUserQrToken(UUID userId) {
        var subscription = subscriptionRepository.findLatestByUserId(new UserId(userId))
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "Subscription not found",
                        HttpStatus.NOT_FOUND
                ));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCodes.FORBIDDEN,
                    "Only ACTIVE subscriptions can issue entrance token",
                    HttpStatus.FORBIDDEN
            );
        }

        entranceValidator.validate(subscription, Instant.now());
        return qrTokenGenerator.generate(userId, subscription.getId().value());
    }

    public void openDoor(OpenDoorCommand command) {
        String requestId = resolveRequestId();
        try {
            QrTokenClaims qrTokenClaims = jwtTokenService.parseQrToken(command.qrToken());
            validateAuthenticatedUser(command.userId(), qrTokenClaims.userId());

            performDoorOpen(command.userId(), qrTokenClaims, requestId);

            auditLogJdbcRepository.save(command.userId(), OPEN_DOOR_ACTION, "SUCCESS", "Door request accepted", requestId);
        } catch (ExpiredJwtException exception) {
            throw logAndWrap(command.userId(), requestId, ErrorCodes.UNAUTHORIZED, "QR token has expired", HttpStatus.UNAUTHORIZED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw logAndWrap(command.userId(), requestId, ErrorCodes.UNAUTHORIZED, "Invalid QR token", HttpStatus.UNAUTHORIZED);
        } catch (BusinessException exception) {
            auditLogJdbcRepository.save(command.userId(), OPEN_DOOR_ACTION, "FAILURE", exception.getMessage(), requestId);
            throw exception;
        } catch (InfrastructureException exception) {
            auditLogJdbcRepository.save(command.userId(), OPEN_DOOR_ACTION, "FAILURE", exception.getMessage(), requestId);
            throw exception;
        } catch (IllegalStateException exception) {
            auditLogJdbcRepository.save(command.userId(), OPEN_DOOR_ACTION, "FAILURE", exception.getMessage(), requestId);
            throw exception;
        }
    }

    public void openDoorByAdminScan(String qrToken) {
        String requestId = resolveRequestId();
        try {
            QrTokenClaims qrTokenClaims = jwtTokenService.parseQrToken(qrToken);

            performDoorOpen(qrTokenClaims.userId(), qrTokenClaims, requestId);

            auditLogJdbcRepository.save(qrTokenClaims.userId(), "ADMIN_SCAN_OPEN_DOOR", "SUCCESS", "Admin scanner door request accepted", requestId);
        } catch (ExpiredJwtException exception) {
            throw logAndWrap(null, requestId, ErrorCodes.UNAUTHORIZED, "QR token has expired", HttpStatus.UNAUTHORIZED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw logAndWrap(null, requestId, ErrorCodes.UNAUTHORIZED, "Invalid QR token", HttpStatus.UNAUTHORIZED);
        } catch (BusinessException exception) {
            auditLogJdbcRepository.save(null, "ADMIN_SCAN_OPEN_DOOR", "FAILURE", exception.getMessage(), requestId);
            throw exception;
        } catch (InfrastructureException exception) {
            auditLogJdbcRepository.save(null, "ADMIN_SCAN_OPEN_DOOR", "FAILURE", exception.getMessage(), requestId);
            throw exception;
        } catch (IllegalStateException exception) {
            auditLogJdbcRepository.save(null, "ADMIN_SCAN_OPEN_DOOR", "FAILURE", exception.getMessage(), requestId);
            throw exception;
        }
    }

    private void performDoorOpen(UUID userId, QrTokenClaims qrTokenClaims, String requestId) {

            // 使い捨て QR を再利用可能にすると、スクリーンショット共有だけで第三者が開錠できる。
            // そのため、QR 自体を短寿命かつ一回限りとして消費する。
            ensureQrTokenNotReplayed(qrTokenClaims);

            // モバイル回線の再送や多重タップは通常運用で発生するため、同一 request_id の 5 秒再送は
            // 異常操作ではなく「同一要求」とみなし二重開錠を防ぐ。
            ensureDoorRequestIsIdempotent(requestId);

            var subscription = subscriptionRepository.findById(new SubscriptionId(qrTokenClaims.subscriptionId()))
                    .orElseThrow(() -> new BusinessException(
                            ErrorCodes.RESOURCE_NOT_FOUND,
                            "Subscription not found",
                            HttpStatus.NOT_FOUND
                    ));

            if (!subscription.getUserId().value().equals(userId)) {
                throw new BusinessException(
                        ErrorCodes.FORBIDDEN,
                        "Subscription does not belong to the QR token user",
                        HttpStatus.FORBIDDEN
                );
            }

            entranceValidator.validate(subscription, Instant.now());

            doorLockClient.unlock("MAIN_DOOR_01");
    }

    private void validateAuthenticatedUser(UUID authenticatedUserId, UUID qrUserId) {
        if (!authenticatedUserId.equals(qrUserId)) {
            throw new BusinessException(
                    ErrorCodes.FORBIDDEN,
                    "QR token does not belong to the authenticated user",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void ensureQrTokenNotReplayed(QrTokenClaims qrTokenClaims) {
        long replayWindowSeconds = Math.max(1L, Duration.between(Instant.now(), qrTokenClaims.expiresAt()).getSeconds());
        boolean accepted = idempotencyService.acquire("qr:" + qrTokenClaims.tokenId(), replayWindowSeconds);
        if (!accepted) {
            throw new BusinessException(
                    ErrorCodes.DUPLICATE_REQUEST,
                    "QR token has already been used",
                    HttpStatus.CONFLICT
            );
        }
    }

    private void ensureDoorRequestIsIdempotent(String requestId) {
        boolean accepted = idempotencyService.acquire("door:" + requestId, 5L);
        if (!accepted) {
            throw new BusinessException(
                    ErrorCodes.DUPLICATE_REQUEST,
                    "Duplicate door request",
                    HttpStatus.CONFLICT
            );
        }
    }

    private String resolveRequestId() {
        String requestId = MDC.get(RequestIdFilter.REQUEST_ID);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    private BusinessException logAndWrap(
            UUID userId,
            String requestId,
            String code,
            String message,
            HttpStatus status
    ) {
        auditLogJdbcRepository.save(userId, OPEN_DOOR_ACTION, "FAILURE", message, requestId);
        return new BusinessException(code, message, status);
    }
}
