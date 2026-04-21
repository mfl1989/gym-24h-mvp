package com.gym24h.infrastructure.cache.redis;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Long> localFallback = new ConcurrentHashMap<>();

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquire(String key, long ttlSeconds) {
        try {
            Boolean stored = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(stored);
        } catch (DataAccessException | IllegalStateException ex) {
            long now = System.currentTimeMillis();
            localFallback.entrySet().removeIf(entry -> entry.getValue() < now);
            return localFallback.putIfAbsent(key, now + Duration.ofSeconds(ttlSeconds).toMillis()) == null;
        }
    }
}
