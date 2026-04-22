package com.gym24h.presentation.controller;

import com.gym24h.application.command.dto.OpenDoorCommand;
import com.gym24h.application.command.service.EntranceCommandService;
import com.gym24h.common.constant.ErrorCodes;
import com.gym24h.common.exception.BusinessException;
import com.gym24h.domain.service.GeneratedQrToken;
import com.gym24h.infrastructure.security.AuthenticatedUser;
import com.gym24h.presentation.request.OpenDoorRequest;
import com.gym24h.presentation.response.ApiResponse;
import com.gym24h.presentation.response.QrTokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/entrances")
public class EntranceController {

    private final EntranceCommandService entranceCommandService;

    public EntranceController(EntranceCommandService entranceCommandService) {
        this.entranceCommandService = entranceCommandService;
    }

    @PostMapping("/open")
    public ApiResponse<String> open(@Valid @RequestBody OpenDoorRequest request) {
        entranceCommandService.openDoor(new OpenDoorCommand(
                currentUserId(),
                request.qrToken()
        ));
        return ApiResponse.ok("OPEN_REQUEST_ACCEPTED");
    }

    @PostMapping("/qr-tokens/{subscriptionId}")
    public ApiResponse<QrTokenResponse> issueQrToken(@PathVariable java.util.UUID subscriptionId) {
        GeneratedQrToken generatedQrToken = entranceCommandService.issueQrToken(currentUserId(), subscriptionId);
        return ApiResponse.ok(new QrTokenResponse(
                generatedQrToken.token(),
                generatedQrToken.tokenId(),
                generatedQrToken.expiresAt()
        ));
    }

    private java.util.UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(
                    ErrorCodes.UNAUTHORIZED,
                    "Authentication is required to open the door",
                    HttpStatus.UNAUTHORIZED
            );
        }
        return authenticatedUser.userId();
    }
}
