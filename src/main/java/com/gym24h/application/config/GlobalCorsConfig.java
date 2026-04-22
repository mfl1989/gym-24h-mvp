package com.gym24h.application.config; // 注意替换成你实际的包名

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class GlobalCorsConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // 绝对最高优先级！在 Security 之前执行
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的来源，* 代表允许所有
        config.addAllowedOriginPattern("*");
        // 允许的请求头
        config.addAllowedHeader("*");
        // 允许的方法 (GET, POST, OPTIONS 等)
        config.addAllowedMethod("*");
        // 允许携带凭证 (Token/Cookie)
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口生效
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}