package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.followup.FollowupAnswersRequestDto;
import com.uit.petrescueapi.application.dto.followup.FollowupDto;
import com.uit.petrescueapi.application.dto.followup.FollowupFormDto;
import com.uit.petrescueapi.application.dto.followup.FollowupRespondRequestDto;
import com.uit.petrescueapi.application.port.command.FollowupCommandPort;
import com.uit.petrescueapi.application.port.query.FollowupQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Adoption follow-up")
public class FollowupController {

    private final FollowupCommandPort commandPort;
    private final FollowupQueryPort queryPort;

    @GetMapping("/followups/respond")
    @Operation(summary = "Open the follow-up form from the link in the email",
            description = "Public. The token identifies one milestone, works until it is answered or expires, and shows only the pet's name and the questions.")
    public ResponseEntity<ApiResponse<FollowupFormDto>> form(@RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.formFor(token)));
    }

    @PostMapping("/followups/respond")
    @Operation(summary = "Answer from the link in the email", description = "Public. A link can be used once.")
    public ResponseEntity<ApiResponse<Void>> respond(@Valid @RequestBody FollowupRespondRequestDto request) {
        commandPort.respondWithToken(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/followups/{id}/answers")
    @Operation(summary = "Answer a follow-up while signed in", description = "Only the adopter the question was sent to.")
    public ResponseEntity<ApiResponse<Void>> answer(@PathVariable UUID id,
                                                    @Valid @RequestBody FollowupAnswersRequestDto answers,
                                                    Authentication authentication) {
        commandPort.respond(id, UUID.fromString(authentication.getName()), answers);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/adoptions/{id}/followups")
    @Operation(summary = "Timeline of follow-up for an adoption",
            description = "The adopter, the organization that handed the pet over, and admins.")
    public ResponseEntity<ApiResponse<List<FollowupDto>>> timeline(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.timeline(id, UUID.fromString(authentication.getName()))));
    }
}
