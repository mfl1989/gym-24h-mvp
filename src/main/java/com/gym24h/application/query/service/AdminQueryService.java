package com.gym24h.application.query.service;

import com.gym24h.application.query.dto.AdminUserProfileView;
import com.gym24h.domain.repository.SubscriptionRepository;
import com.gym24h.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Admin 向け一覧参照をまとめる Query Service。
 *
 * 運営画面では個別ユーザーの認証主体ではなく、全体の利用状態を俯瞰することが目的であるため、
 * ユーザー基本情報と最新購読状態を一つの読み取り DTO に整形して返す。
 */
@Service
public class AdminQueryService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    public AdminQueryService(UserRepository userRepository, SubscriptionRepository subscriptionRepository) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public List<AdminUserProfileView> getAllUsersWithStatus() {
        return userRepository.findAll().stream()
                .map(user -> new AdminUserProfileView(
                        user.getId().value(),
                        user.getLineUserId(),
                        user.getDisplayName(),
                        user.getMembershipStatus(),
                        subscriptionRepository.findLatestByUserId(user.getId())
                                .map(com.gym24h.domain.model.subscription.Subscription::getStatus)
                                .orElse(null)
                ))
                .toList();
    }
}
