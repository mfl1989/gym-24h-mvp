package com.gym24h.presentation.controller;

import com.gym24h.application.command.dto.OpenDoorCommand;
import com.gym24h.application.command.service.EntranceCommandService;
import com.gym24h.presentation.request.OpenDoorRequest;
import com.gym24h.presentation.response.ApiResponse;
import jakarta.validation.Valid;
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
                request.userId(),
                request.subscriptionId(),
                request.requestId(),
                request.requestedAt()
        ));
        return ApiResponse.ok("OPEN_REQUEST_ACCEPTED");
    }
}
