package com.gym24h.presentation.controller;

import com.gym24h.application.query.dto.CurrentUserProfileView;
import com.gym24h.application.query.dto.UserInvoiceHistoryItemView;
import com.gym24h.application.query.service.UserProfileQueryService;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.infrastructure.security.AuthenticatedUser;
import com.gym24h.presentation.response.ApiResponse;
import com.gym24h.presentation.response.MeInvoiceItemResponse;
import com.gym24h.presentation.response.MeProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
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

    public MeController(UserProfileQueryService userProfileQueryService) {
        this.userProfileQueryService = userProfileQueryService;
    }

    @GetMapping
    public ApiResponse<MeProfileResponse> getCurrentUserProfile() {
        CurrentUserProfileView profile = userProfileQueryService.getCurrentUserProfile(currentUserId());
        return ApiResponse.ok(new MeProfileResponse(
                profile.displayName(),
                profile.membershipStatus(),
                profile.subscriptionStatus(),
                profile.subscriptionValidUntil()
        ));
    }

    @GetMapping("/invoices")
    public ApiResponse<List<MeInvoiceItemResponse>> getUserInvoiceHistory() {
        List<MeInvoiceItemResponse> response = userProfileQueryService.getUserInvoiceHistory(currentUserId()).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(response);
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
