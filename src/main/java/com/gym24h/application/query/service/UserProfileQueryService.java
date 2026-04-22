package com.gym24h.application.query.service;

import com.gym24h.application.query.dto.CurrentUserProfileView;
import com.gym24h.application.query.dto.UserInvoiceHistoryItemView;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.domain.model.invoice.Invoice;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.InvoiceRepository;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.domain.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * ログイン中ユーザー向けの参照モデルを組み立てる Query Service。
 *
 * フロントエンドには内部主キーや更新用の内部状態を見せず、
 * 「現在の利用状態」と「請求履歴」を読み取り専用の DTO に整形して返す責務を持つ。
 */
@Service
public class UserProfileQueryService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;

    public UserProfileQueryService(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public CurrentUserProfileView getCurrentUserProfile(UUID userIdValue) {
        UserId userId = new UserId(userIdValue);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.RESOURCE_NOT_FOUND,
                        "User not found",
                        HttpStatus.NOT_FOUND
                ));

        var subscription = subscriptionRepository.findLatestByUserId(userId);
        return new CurrentUserProfileView(
                user.getDisplayName(),
                user.getMembershipStatus(),
                subscription.map(com.gym24h.domain.model.subscription.Subscription::getStatus).orElse(null),
                subscription.map(com.gym24h.domain.model.subscription.Subscription::getValidUntil).orElse(null)
        );
    }

    public List<UserInvoiceHistoryItemView> getUserInvoiceHistory(UUID userIdValue) {
        return invoiceRepository.findByUserIdOrderByBilledAtDesc(new UserId(userIdValue)).stream()
                .map(this::toInvoiceHistoryItemView)
                .toList();
    }

    private UserInvoiceHistoryItemView toInvoiceHistoryItemView(Invoice invoice) {
        return new UserInvoiceHistoryItemView(
                invoice.getStatus(),
                formatAmount(invoice.getAmountPaid()),
                invoice.getCurrency().toUpperCase(),
                invoice.getInvoiceUrl(),
                invoice.getBilledAt(),
                invoice.getPaidAt(),
                invoice.getDueAt()
        );
    }

    private String formatAmount(int amountMinorUnit) {
        return BigDecimal.valueOf(amountMinorUnit)
                .movePointLeft(2)
                .setScale(2, RoundingMode.UNNECESSARY)
                .toPlainString();
    }
}
