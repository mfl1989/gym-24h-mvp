package com.gym24h.presentation.controller;

import com.gym24h.application.command.service.EntranceCommandService;
import com.gym24h.application.command.service.SubscriptionCommandService;
import com.gym24h.application.query.dto.CurrentUserProfileView;
import com.gym24h.application.query.dto.MySubscriptionDetailsView;
import com.gym24h.application.query.dto.UserInvoiceHistoryItemView;
import com.gym24h.application.query.service.UserProfileQueryService;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.infrastructure.security.AuthenticatedUser;
import com.gym24h.presentation.response.ApiResponse;
import com.gym24h.presentation.response.EntranceTokenResponse;
import com.gym24h.presentation.response.MeInvoiceItemResponse;
import com.gym24h.presentation.response.MeProfileResponse;
import com.gym24h.presentation.response.MySubscriptionDetailsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * ログイン中ユーザー自身の参照 API を提供するコントローラー。
 *
 * userId を URL や request body から受け取ると他人のデータ参照に悪用されるため、
 * 認証済み principal からのみ userId を取り出す。
 */
@RestController
@RequestMapping("/me")
public class MeController {

    private final UserProfileQueryService userProfileQueryService;
    private final SubscriptionCommandService subscriptionCommandService;
    private final EntranceCommandService entranceCommandService;

    public MeController(
            UserProfileQueryService userProfileQueryService,
            SubscriptionCommandService subscriptionCommandService,
            EntranceCommandService entranceCommandService
    ) {
        this.userProfileQueryService = userProfileQueryService;
        this.subscriptionCommandService = subscriptionCommandService;
        this.entranceCommandService = entranceCommandService;
    }

    @GetMapping
    public ApiResponse<MeProfileResponse> getCurrentUserProfile() {
        CurrentUserProfileView profile = userProfileQueryService.getCurrentUserProfile(currentUserId());
        return ApiResponse.ok(new MeProfileResponse(
                profile.displayName(),
                profile.membershipStatus(),
                profile.subscriptionStatus(),
            profile.subscriptionValidUntil(),
            profile.cancelAtPeriodEnd()
        ));
    }

    @GetMapping("/invoices")
    public ApiResponse<List<MeInvoiceItemResponse>> getUserInvoiceHistory() {
        List<MeInvoiceItemResponse> response = userProfileQueryService.getUserInvoiceHistory(currentUserId()).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(response);
    }

    @GetMapping("/subscription")
    public ApiResponse<MySubscriptionDetailsResponse> getMySubscriptionDetails() {
        MySubscriptionDetailsView details = userProfileQueryService.getMySubscriptionDetails(currentUserId());
        return ApiResponse.ok(new MySubscriptionDetailsResponse(
                details.status(),
                details.currentPeriodStartAt(),
                details.currentPeriodEndAt(),
                details.cancelAtPeriodEnd(),
                details.withinCancellationWindow()
        ));
    }

    @GetMapping("/entrance-token")
    public ApiResponse<EntranceTokenResponse> getEntranceToken() {
        var generatedQrToken = entranceCommandService.issueCurrentUserQrToken(currentUserId());
        return ApiResponse.ok(new EntranceTokenResponse(generatedQrToken.token()));
    }

    @PostMapping("/subscription/cancel")
    public ApiResponse<String> cancelSubscriptionAtPeriodEnd() {
        subscriptionCommandService.cancelSubscriptionAtPeriodEnd(currentUserId());
        return ApiResponse.ok("CANCELLATION_REQUEST_ACCEPTED");
    }

    @PostMapping("/subscription/revoke-cancel")
    public ApiResponse<String> revokeSubscriptionCancellation() {
        subscriptionCommandService.revokeSubscriptionCancellation(currentUserId());
        return ApiResponse.ok("CANCELLATION_REVOCATION_ACCEPTED");
    }

    private MeInvoiceItemResponse toResponse(UserInvoiceHistoryItemView invoice) {
        return new MeInvoiceItemResponse(
                invoice.status(),
                invoice.amount(),
                invoice.currency(),
                invoice.invoiceUrl(),
                invoice.billedAt(),
                invoice.paidAt(),
                invoice.dueAt()
        );
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(
                    ErrorCodes.UNAUTHORIZED,
                    "Authentication is required",
                    HttpStatus.UNAUTHORIZED
            );
        }
        return authenticatedUser.userId();
    }
}
