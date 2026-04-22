package com.gym24h.infrastructure.security;

import com.gym24h.presentation.response.AuthTokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final long DEFAULT_AUTH_TTL_SECONDS = 3600L;

    private final SecretKey authSecretKey;
    private final SecretKey qrSecretKey;
    private final String authIssuer;
    private final String qrIssuer;
    private final Clock clock;
    private final Duration authTtl;

    @Autowired
    public JwtTokenService(
            @Value("${security.jwt.auth-secret:dev-auth-secret-32chars-minimum-length-value}") String authSecret,
            @Value("${security.qr.secret:dev-qr-secret-32chars-minimum-length-value!!}") String qrSecret,
            @Value("${security.jwt.issuer:gym24h-auth}") String authIssuer,
            @Value("${security.qr.issuer:gym24h-qr}") String qrIssuer,
            Clock clock,
            @Value("${security.jwt.auth-ttl-seconds:3600}") long authTtlSeconds
    ) {
        this(authSecret, qrSecret, authIssuer, qrIssuer, clock, Duration.ofSeconds(authTtlSeconds));
    }

    public JwtTokenService(
            String authSecret,
            String qrSecret,
            String authIssuer,
            String qrIssuer,
            Clock clock,
            Duration authTtl
    ) {
        this.authSecretKey = Keys.hmacShaKeyFor(authSecret.getBytes(StandardCharsets.UTF_8));
        this.qrSecretKey = Keys.hmacShaKeyFor(qrSecret.getBytes(StandardCharsets.UTF_8));
        this.authIssuer = authIssuer;
        this.qrIssuer = qrIssuer;
        this.clock = clock;
        this.authTtl = authTtl;
    }

    public AuthTokenResponse issueAuthenticationToken(UUID userId) {
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(authTtl);
        String token = Jwts.builder()
                .issuer(authIssuer)
                .subject(userId.toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("userId", userId.toString())
                .signWith(authSecretKey, Jwts.SIG.HS256)
                .compact();
        return new AuthTokenResponse(token, expiresAt);
    }

    public AuthenticatedUser parseAuthenticationToken(String token) {
        Claims claims = parseClaims(token, authSecretKey, authIssuer);
        return new AuthenticatedUser(extractUserId(claims));
    }

    public QrTokenClaims parseQrToken(String token) {
        Claims claims = parseClaims(token, qrSecretKey, qrIssuer);
        Date expiresAt = claims.getExpiration();
        if (expiresAt == null) {
            throw new JwtException("QR token expiration is required");
        }

        String tokenId = claims.getId();
        if (tokenId == null || tokenId.isBlank()) {
            throw new JwtException("QR token id is required");
        }

        String subscriptionIdValue = claims.get("subscriptionId", String.class);
        if (subscriptionIdValue == null || subscriptionIdValue.isBlank()) {
            throw new JwtException("QR token subscriptionId is required");
        }

        return new QrTokenClaims(
                extractUserId(claims),
                UUID.fromString(subscriptionIdValue),
                tokenId,
                expiresAt.toInstant()
        );
    }

    private Claims parseClaims(String token, SecretKey secretKey, String expectedIssuer) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(expectedIssuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private UUID extractUserId(Claims claims) {
        String value = claims.getSubject();
        if (value == null || value.isBlank()) {
            value = claims.get("userId", String.class);
        }
        if (value == null || value.isBlank()) {
            throw new JwtException("JWT userId is required");
        }
        return UUID.fromString(value);
    }
}
