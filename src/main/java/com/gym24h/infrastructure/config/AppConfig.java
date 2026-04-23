package com.gym24h.infrastructure.config;

import com.gym24h.domain.service.EntranceValidator;
import com.gym24h.domain.service.QrTokenGenerator;
import com.gym24h.domain.service.SubscriptionDomainService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;

@EnableAsync
@EnableScheduling
@Configuration
public class AppConfig {

    @Bean
    @Qualifier("doorLockRestTemplate")
    public RestTemplate doorLockRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Bean
    @Qualifier("lineApiRestTemplate")
    public RestTemplate lineApiRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public EntranceValidator entranceValidator() {
        return new EntranceValidator();
    }

    @Bean
    public SubscriptionDomainService subscriptionDomainService() {
        return new SubscriptionDomainService();
    }

    @Bean
    public QrTokenGenerator qrTokenGenerator(
            Clock clock,
            @Value("${security.qr.secret:dev-qr-secret-32chars-minimum-length-value!!}") String qrSecret,
            @Value("${security.qr.issuer:gym24h-qr}") String qrIssuer,
            @Value("${security.qr.ttl-seconds:30}") long ttlSeconds
    ) {
        return new QrTokenGenerator(qrSecret, qrIssuer, clock, Duration.ofSeconds(ttlSeconds));
    }
}
