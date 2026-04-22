package com.gym24h.domain.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

public class QrTokenGenerator {

    private static final long MAX_TTL_SECONDS = 30L;

    private final SecretKey secretKey;
    private final String issuer;
    private final Clock clock;
    private final Duration ttl;

    public QrTokenGenerator(String secret, String issuer, Clock clock, Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero() || ttl.getSeconds() > MAX_TTL_SECONDS) {
            throw new IllegalArgumentException("QR token ttl must be between 1 and 30 seconds");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.clock = clock;
        this.ttl = ttl;
    }

    public GeneratedQrToken generate(UUID userId, UUID subscriptionId) {
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(ttl);
        UUID tokenId = UUID.randomUUID();

        String token = Jwts.builder()
            .issuer(issuer)
                .subject(userId.toString())
                .id(tokenId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("userId", userId.toString())
                .claim("subscriptionId", subscriptionId.toString())
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        return new GeneratedQrToken(token, tokenId, expiresAt);
    }
}
