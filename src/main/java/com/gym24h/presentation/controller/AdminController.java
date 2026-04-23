package com.gym24h.presentation.controller;

import com.gym24h.application.command.service.SubscriptionCommandService;
import com.gym24h.application.command.service.EntranceCommandService;
import com.gym24h.application.outbound.DoorLockClient;
import com.gym24h.application.query.dto.AdminUserProfileView;
import com.gym24h.application.query.service.AdminQueryService;
import com.gym24h.common.logging.RequestIdFilter;
import com.gym24h.infrastructure.persistence.repository.AuditLogJdbcRepository;
import com.gym24h.presentation.request.AdminRemoteOpenRequest;
import com.gym24h.presentation.request.AdminScanEntranceRequest;
import com.gym24h.presentation.request.ForceTerminateSubscriptionRequest;
import com.gym24h.presentation.response.AdminUserProfileResponse;
import com.gym24h.presentation.response.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 管理者専用 API。
 *
 * MVP では管理者認証を共通 secret で最小実装し、運営に必要な俯瞰参照と緊急停止だけを提供する。
 * 課金系の Stripe 側整合は後段で処理しても、物理入館権限だけは即時遮断できることを優先する。
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminQueryService adminQueryService;
    private final DoorLockClient doorLockClient;
    private final SubscriptionCommandService subscriptionCommandService;
    private final EntranceCommandService entranceCommandService;
    private final AuditLogJdbcRepository auditLogJdbcRepository;

    public AdminController(
            AdminQueryService adminQueryService,
            DoorLockClient doorLockClient,
            SubscriptionCommandService subscriptionCommandService,
            EntranceCommandService entranceCommandService,
            AuditLogJdbcRepository auditLogJdbcRepository
    ) {
        this.adminQueryService = adminQueryService;
        this.doorLockClient = doorLockClient;
        this.subscriptionCommandService = subscriptionCommandService;
        this.entranceCommandService = entranceCommandService;
        this.auditLogJdbcRepository = auditLogJdbcRepository;
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserProfileResponse>> getAllUsers() {
        List<AdminUserProfileResponse> response = adminQueryService.getAllUsersWithStatus().stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(response);
    }

    @PostMapping("/entrances/remote-open")
    public ApiResponse<String> remoteOpen(@Valid @RequestBody AdminRemoteOpenRequest request) {
        String requestId = resolveRequestId();
        try {
            doorLockClient.unlock(request.deviceId());
            auditLogJdbcRepository.save(null, "ADMIN_REMOTE_OPEN", "SUCCESS", request.deviceId(), requestId);
            return ApiResponse.ok("REMOTE_OPEN_ACCEPTED");
        } catch (RuntimeException exception) {
            auditLogJdbcRepository.save(null, "ADMIN_REMOTE_OPEN", "FAILURE", exception.getMessage(), requestId);
            throw exception;
        }
    }

    @PostMapping("/entrances/scan")
    public ApiResponse<String> scanEntrance(@Valid @RequestBody AdminScanEntranceRequest request) {
        entranceCommandService.openDoorByAdminScan(request.qrToken());
        return ApiResponse.ok("SCAN_OPEN_ACCEPTED");
    }

    @PostMapping("/subscriptions/{userId}/terminate")
    public ApiResponse<String> forceTerminateSubscription(
            @PathVariable UUID userId,
            @Valid @RequestBody ForceTerminateSubscriptionRequest request
    ) {
        subscriptionCommandService.forceTerminateSubscription(userId, request.reason());
        return ApiResponse.ok("FORCE_TERMINATION_ACCEPTED");
    }

    private AdminUserProfileResponse toResponse(AdminUserProfileView view) {
        return new AdminUserProfileResponse(
                view.userId(),
                view.lineUserId(),
                view.displayName(),
                view.membershipStatus(),
                view.subscriptionStatus()
        );
    }

    private String resolveRequestId() {
        String requestId = MDC.get(RequestIdFilter.REQUEST_ID);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
