package com.gym24h.application.command.service;

import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.domain.model.user.User;
import com.gym24h.domain.repository.UserRepository;
import com.gym24h.infrastructure.security.JwtTokenService;
import com.gym24h.presentation.response.AuthTokenResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class LineLoginService {

    private static final String LINE_VERIFY_URL = "https://api.line.me/oauth2/v2.1/verify";
    private static final String LINE_ISSUER = "https://access.line.me";

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final String lineChannelId;

    public LineLoginService(
            @Qualifier("lineApiRestTemplate") RestTemplate restTemplate,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            @Value("${line.channel-id}") String lineChannelId
    ) {
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.lineChannelId = lineChannelId;
    }

    @Transactional
    public AuthTokenResponse login(String idToken) {
        VerifiedLineIdentity identity = verifyIdToken(idToken);
        User user = userRepository.findByLineUserId(identity.lineUserId())
                .map(existingUser -> syncLineProfile(existingUser, identity))
                .orElseGet(() -> userRepository.save(User.create(
                        identity.lineUserId(),
                        identity.displayName(),
                        identity.pictureUrl()
                )));
        return jwtTokenService.issueAuthenticationToken(user.getId().value());
    }

    private VerifiedLineIdentity verifyIdToken(String idToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("id_token", idToken);
        requestBody.add("client_id", lineChannelId);

        try {
            ResponseEntity<LineVerifyResponse> response = restTemplate.exchange(
                    LINE_VERIFY_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    LineVerifyResponse.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw unauthorizedLineToken();
            }

            LineVerifyResponse verified = response.getBody();
            validateVerifiedIdentity(verified);
            return new VerifiedLineIdentity(
                    verified.sub(),
                    verified.name(),
                    verified.picture()
            );
        } catch (HttpStatusCodeException exception) {
            throw unauthorizedLineToken();
        } catch (ResourceAccessException exception) {
            throw lineProviderUnavailable();
        }
    }

    private void validateVerifiedIdentity(LineVerifyResponse verified) {
        if (verified.sub() == null || verified.sub().isBlank()) {
            throw unauthorizedLineToken();
        }

        if (verified.aud() == null || !lineChannelId.equals(verified.aud())) {
            throw unauthorizedLineToken();
        }

        if (verified.iss() == null || !LINE_ISSUER.equals(verified.iss())) {
            throw unauthorizedLineToken();
        }

        if (verified.exp() == null || verified.exp() <= Instant.now().getEpochSecond()) {
            throw unauthorizedLineToken();
        }
    }

    private User syncLineProfile(User existingUser, VerifiedLineIdentity identity) {
        boolean displayNameChanged = !Objects.equals(existingUser.getDisplayName(), identity.displayName());
        boolean pictureChanged = !Objects.equals(existingUser.getSubChar1(), identity.pictureUrl());
        if (!displayNameChanged && !pictureChanged) {
            return existingUser;
        }

        User updatedUser = existingUser.toBuilder()
                .displayName(identity.displayName())
                .subChar1(identity.pictureUrl())
                .updatedAt(Instant.now())
                .build();
        return userRepository.save(updatedUser);
    }

    private BusinessException unauthorizedLineToken() {
        return new BusinessException(
                ErrorCodes.UNAUTHORIZED,
                "Invalid LINE ID token",
                HttpStatus.UNAUTHORIZED
        );
    }

    private BusinessException lineProviderUnavailable() {
        return new BusinessException(
                ErrorCodes.INTERNAL_ERROR,
                "LINE verification is temporarily unavailable",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    private record LineVerifyResponse(
            String iss,
            String sub,
            String aud,
            Long exp,
            String nonce,
            String amr,
            String name,
            String picture
    ) {
    }

    private record VerifiedLineIdentity(String lineUserId, String displayName, String pictureUrl) {
    }
}