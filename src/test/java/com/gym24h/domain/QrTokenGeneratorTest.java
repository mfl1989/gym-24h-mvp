package com.gym24h.domain;

import com.gym24h.domain.service.GeneratedQrToken;
import com.gym24h.domain.service.QrTokenGenerator;
import com.gym24h.infrastructure.security.JwtTokenService;
import com.gym24h.infrastructure.security.QrTokenClaims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QrTokenGeneratorTest {

    private static final String QR_SECRET = "dev-qr-secret-32chars-minimum-length-value!!";
        private static final String AUTH_SECRET = "dev-auth-secret-32chars-minimum-length-value";
        private static final String AUTH_ISSUER = "gym24h-auth";
        private static final String QR_ISSUER = "gym24h-qr";

    @Test
    void shouldGenerateTokenRecognizedByExistingParser() {
                Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        QrTokenGenerator generator = new QrTokenGenerator(
                QR_SECRET,
                QR_ISSUER,
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofSeconds(30)
        );
        JwtTokenService jwtTokenService = new JwtTokenService(
                AUTH_SECRET,
                QR_SECRET,
                AUTH_ISSUER,
                QR_ISSUER,
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofHours(1)
        );
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        GeneratedQrToken generatedQrToken = generator.generate(userId, subscriptionId);
        QrTokenClaims claims = jwtTokenService.parseQrToken(generatedQrToken.token());

        assertEquals(userId, claims.userId());
        assertEquals(subscriptionId, claims.subscriptionId());
        assertEquals(generatedQrToken.tokenId().toString(), claims.tokenId());
        assertEquals(now.plusSeconds(30), claims.expiresAt());
    }

    @Test
    void shouldExpireAfterThirtyOneSeconds() {
        Instant expiredBase = Instant.now().minusSeconds(31);
        QrTokenGenerator generator = new QrTokenGenerator(
                QR_SECRET,
                QR_ISSUER,
                Clock.fixed(expiredBase, ZoneOffset.UTC),
                Duration.ofSeconds(30)
        );
        JwtTokenService jwtTokenService = new JwtTokenService(
                AUTH_SECRET,
                QR_SECRET,
                AUTH_ISSUER,
                QR_ISSUER,
                Clock.fixed(Instant.now(), ZoneOffset.UTC),
                Duration.ofHours(1)
        );

        GeneratedQrToken generatedQrToken = generator.generate(UUID.randomUUID(), UUID.randomUUID());

        assertThrows(ExpiredJwtException.class, () -> jwtTokenService.parseQrToken(generatedQrToken.token()));
    }
}