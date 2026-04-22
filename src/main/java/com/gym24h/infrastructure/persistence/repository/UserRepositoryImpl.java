package com.gym24h.infrastructure.persistence.repository;

import com.gym24h.domain.model.user.User;
import com.gym24h.domain.model.user.UserId;
import com.gym24h.domain.repository.UserRepository;
import com.gym24h.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    public UserRepositoryImpl(JpaUserRepository jpaUserRepository) {
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId userId) {
        return jpaUserRepository.findByIdAndDeletedFalse(userId.value())
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return jpaUserRepository.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity savedEntity = jpaUserRepository.save(toEntity(user));
        return toDomain(savedEntity);
    }

    private User toDomain(UserEntity entity) {
        return User.builder()
                .id(new UserId(entity.getId()))
                .lineUserId(entity.getLineUserId())
                .phoneNumber(entity.getPhoneNumber())
                .displayName(entity.getDisplayName())
                .membershipStatus(entity.getMembershipStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .deleted(entity.isDeleted())
                .subChar1(entity.getSubChar1())
                .subChar2(entity.getSubChar2())
                .subChar3(entity.getSubChar3())
                .subChar4(entity.getSubChar4())
                .subChar5(entity.getSubChar5())
                .subChar6(entity.getSubChar6())
                .subChar7(entity.getSubChar7())
                .subChar8(entity.getSubChar8())
                .subChar9(entity.getSubChar9())
                .subChar10(entity.getSubChar10())
                .build();
    }

    private UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId().value());
        entity.setLineUserId(user.getLineUserId());
        entity.setPhoneNumber(user.getPhoneNumber());
        entity.setDisplayName(user.getDisplayName());
        entity.setMembershipStatus(user.getMembershipStatus());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        entity.setVersion(user.getVersion());
        entity.setDeleted(user.isDeleted());
        entity.setSubChar1(user.getSubChar1());
        entity.setSubChar2(user.getSubChar2());
        entity.setSubChar3(user.getSubChar3());
        entity.setSubChar4(user.getSubChar4());
        entity.setSubChar5(user.getSubChar5());
        entity.setSubChar6(user.getSubChar6());
        entity.setSubChar7(user.getSubChar7());
        entity.setSubChar8(user.getSubChar8());
        entity.setSubChar9(user.getSubChar9());
        entity.setSubChar10(user.getSubChar10());
        return entity;
    }
}
