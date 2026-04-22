package com.gym24h.infrastructure.persistence.repository;

import com.gym24h.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByIdAndDeletedFalse(UUID id);

    java.util.List<UserEntity> findByDeletedFalseOrderByCreatedAtDesc();
}
