package com.gym24h.presentation.controller;

import com.gym24h.application.command.service.DebugLoginService;
import com.gym24h.application.command.service.LineLoginService;
import com.gym24h.infrastructure.security.JwtTokenService;
import com.gym24h.presentation.request.DebugLoginRequest;
import com.gym24h.presentation.request.LineLoginRequest;
import com.gym24h.presentation.response.ApiResponse;
import com.gym24h.presentation.response.AuthTokenResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "postgres-local", "test"})
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenService jwtTokenService;
    private final DebugLoginService debugLoginService;
    private final LineLoginService lineLoginService;

    public AuthController(
            JwtTokenService jwtTokenService,
            DebugLoginService debugLoginService,
            LineLoginService lineLoginService
    ) {
        this.jwtTokenService = jwtTokenService;
        this.debugLoginService = debugLoginService;
        this.lineLoginService = lineLoginService;
    }

    @PostMapping("/debug/login")
    public ApiResponse<AuthTokenResponse> debugLogin(@Valid @RequestBody DebugLoginRequest request) {
        return ApiResponse.ok(debugLoginService.loginOrProvision(request.userId()));
    }

    @PostMapping("/dev-login")
    public ApiResponse<AuthTokenResponse> devLogin(@Valid @RequestBody DebugLoginRequest request) {
        return ApiResponse.ok(debugLoginService.loginOrProvision(request.userId()));
    }

    @PostMapping("/line-login")
    public ApiResponse<AuthTokenResponse> lineLogin(@Valid @RequestBody LineLoginRequest request) {
        return ApiResponse.ok(lineLoginService.login(request.idToken()));
    }
}