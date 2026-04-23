package com.gym24h.infrastructure.external.doorlock;

import com.gym24h.application.outbound.DoorLockClient;
import com.gym24h.common.exception.InfrastructureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.util.Map;

/**
 * 物理門禁 API を呼び出す HTTP クライアント。
 *
 * 門が開かないケースはユーザー体験へ直結する一方、無人運営では待たせ続けることも危険であるため、
 * 同期呼び出しは短い timeout と少数回の retry に限定する。長くぶら下げず、到達不能なら即座に失敗として返す。
 */
@Component
@ConditionalOnProperty(name = "door-lock.mock", havingValue = "false", matchIfMissing = true)
public class RemoteDoorLockClientImpl implements DoorLockClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteDoorLockClientImpl.class);

    private final RestTemplate restTemplate;
    private final String apiUrl;

    public RemoteDoorLockClientImpl(
            @Qualifier("doorLockRestTemplate") RestTemplate restTemplate,
            @Value("${door-lock.api-url:http://localhost:8081/mock/door-lock/unlock}") String apiUrl
    ) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    @Override
    @Retryable(
            retryFor = {ResourceAccessException.class, ConnectException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void unlock(String deviceId) {
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(
                    apiUrl,
                    Map.of(
                            "deviceId", deviceId,
                    "command", "UNLOCK"
                ),
                Void.class
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
            throw new InfrastructureException(
                "DOOR_LOCK_OFFLINE",
                "Door lock returned non-success status for deviceId=" + deviceId
            );
            }
        } catch (ResourceAccessException exception) {
            log.warn("door lock api temporarily unavailable deviceId={} apiUrl={}", deviceId, apiUrl);
            throw exception;
        }
    }

    @Recover
    public void recover(ResourceAccessException exception, String deviceId) {
        throw new InfrastructureException(
                "DOOR_LOCK_OFFLINE",
                "Door lock is offline for deviceId=" + deviceId,
                exception
        );
    }

    @Recover
    public void recover(ConnectException exception, String deviceId) {
        throw new InfrastructureException(
                "DOOR_LOCK_OFFLINE",
                "Door lock is offline for deviceId=" + deviceId,
                exception
        );
    }
}