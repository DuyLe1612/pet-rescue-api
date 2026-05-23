package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.port.command.UserCommandPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class PushController {

    private final UserCommandPort userCommandPort;

    @PostMapping("/push-token")
    public ResponseEntity<ApiResponse<String>> updatePushToken(@RequestBody Map<String, String> payload) {
        String token = payload.get("expoPushToken");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(401, "unauthorized"));
        }
        UUID userId = UUID.fromString(auth.getName());
        userCommandPort.updatePushToken(userId, token);
        return ResponseEntity.ok(ApiResponse.ok("ok"));
    }
}
