package com.gym24h.infrastructure.persistence.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class AuditLogJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(UUID userId, String action, String result, String reason, String requestId) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                insert into audit_logs (
                    id, user_id, action, result, reason, request_id,
                    created_at, updated_at, version, is_deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                userId,
                action,
                result,
                reason,
                requestId,
                Timestamp.from(now),
                Timestamp.from(now),
                0,
                false
        );
    }
}