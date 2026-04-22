package com.gym24h;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
public class Gym24hApplication {

    public static void main(String[] args) {
        SpringApplication.run(Gym24hApplication.class, args);
    }
}
