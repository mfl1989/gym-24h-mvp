package com.gym24h.infrastructure.config;

import com.gym24h.domain.service.EntranceValidator;
import com.gym24h.domain.service.SubscriptionDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

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
}
