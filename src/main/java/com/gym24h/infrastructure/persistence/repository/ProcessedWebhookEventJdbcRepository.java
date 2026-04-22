package com.gym24h.infrastructure.persistence.repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class ProcessedWebhookEventJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedWebhookEventJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean tryAcquire(String eventId, String eventType) {
        try {
            jdbcTemplate.update(
                    "insert into processed_webhook_events (event_id, event_type, processed_at) values (?, ?, ?)",
                    eventId,
                    eventType,
                    Timestamp.from(Instant.now())
            );
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }
}
