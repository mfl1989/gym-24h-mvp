package com.gym24h.application.outbound;

import com.gym24h.common.exception.InfrastructureException;
import com.gym24h.infrastructure.config.AppConfig;
import com.gym24h.infrastructure.external.doorlock.RemoteDoorLockClientImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        RemoteDoorLockClientImpl.class,
        AppConfig.class,
        RemoteDoorLockClientTest.RetryTestConfig.class
})
@TestPropertySource(properties = "door-lock.api-url=http://localhost:8081/mock/door-lock/unlock")
class RemoteDoorLockClientTest {

    private static final String API_URL = "http://localhost:8081/mock/door-lock/unlock";
    private static final String DEVICE_ID = "MAIN_DOOR_01";

    @Autowired
    private DoorLockClient doorLockClient;

    @MockBean(name = "doorLockRestTemplate")
    private RestTemplate restTemplate;

    @Test
    void shouldUnlockOnFirstAttempt() {
        when(restTemplate.postForEntity(eq(API_URL), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        assertThatCode(() -> doorLockClient.unlock(DEVICE_ID)).doesNotThrowAnyException();

        verify(restTemplate, times(1)).postForEntity(eq(API_URL), any(), eq(Void.class));
    }

    @Test
    void shouldRetryOnceAndSucceed() {
        when(restTemplate.postForEntity(eq(API_URL), any(), eq(Void.class)))
                .thenThrow(new ResourceAccessException("connect timeout"))
                .thenReturn(ResponseEntity.ok().build());

        assertThatCode(() -> doorLockClient.unlock(DEVICE_ID)).doesNotThrowAnyException();

        verify(restTemplate, times(2)).postForEntity(eq(API_URL), any(), eq(Void.class));
    }

    @Test
    void shouldThrowInfrastructureExceptionAfterAllRetriesFail() {
        when(restTemplate.postForEntity(eq(API_URL), any(), eq(Void.class)))
                .thenThrow(new ResourceAccessException("connect timeout #1"))
                .thenThrow(new ResourceAccessException("connect timeout #2"))
                .thenThrow(new ResourceAccessException("connect timeout #3"));

        assertThatThrownBy(() -> doorLockClient.unlock(DEVICE_ID))
                .isInstanceOf(InfrastructureException.class)
                .hasMessageContaining("Door lock is offline")
                .extracting("code")
                .isEqualTo("DOOR_LOCK_OFFLINE");

        verify(restTemplate, times(3)).postForEntity(eq(API_URL), any(), eq(Void.class));
    }

    @Configuration
    @EnableRetry
    @Import(AppConfig.class)
    static class RetryTestConfig {

                @Bean
                RestTemplateBuilder restTemplateBuilder() {
                        return new RestTemplateBuilder();
                }
    }
}
