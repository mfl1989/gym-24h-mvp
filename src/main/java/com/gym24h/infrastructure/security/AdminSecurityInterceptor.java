package com.gym24h.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.logging.RequestIdFilter;
import com.gym24h.presentation.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

@Component
public class AdminSecurityInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;
    private final String adminSecret;

    public AdminSecurityInterceptor(
            ObjectMapper objectMapper,
            @Value("${app.admin.secret}") String adminSecret
    ) {
        this.objectMapper = objectMapper;
        this.adminSecret = adminSecret;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String adminToken = request.getHeader("X-Admin-Token");
        if (adminToken != null && adminToken.equals(adminSecret)) {
            return true;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                ErrorCodes.UNAUTHORIZED,
                "Invalid admin token",
                Instant.now(),
                MDC.get(RequestIdFilter.REQUEST_ID)
        ));
        return false;
    }
}
