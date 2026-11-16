package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.followup.FollowupAnswersRequestDto;
import com.uit.petrescueapi.application.dto.followup.AdoptionOutcomeDto;
import com.uit.petrescueapi.application.dto.followup.FollowupAlertDto;
import com.uit.petrescueapi.application.dto.followup.FollowupPreferenceDto;
import com.uit.petrescueapi.application.dto.followup.RecordOutcomeRequestDto;
import com.uit.petrescueapi.application.dto.followup.ReturnRequestDto;
import com.uit.petrescueapi.application.dto.followup.SupportRequestDto;
import com.uit.petrescueapi.application.dto.followup.FollowupDto;
import com.uit.petrescueapi.application.dto.followup.UpdateFollowupAlertRequestDto;
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
import org.springframework.web.bind.annotation.PatchMapping;
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

    @GetMapping("/organizations/me/followup-alerts")
    @Operation(summary = "Follow-up work for my organization",
            description = "Red first, then oldest. status=OPEN|IN_PROGRESS|RESOLVED filters. Owners and staff only; admins see every organization.")
    public ResponseEntity<ApiResponse<List<FollowupAlertDto>>> alerts(@RequestParam(required = false) String status,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.alerts(UUID.fromString(authentication.getName()), status)));
    }

    @PatchMapping("/followup-alerts/{id}")
    @Operation(summary = "Take, note or close a follow-up alert")
    public ResponseEntity<ApiResponse<FollowupAlertDto>> updateAlert(@PathVariable UUID id,
                                                                     @Valid @RequestBody UpdateFollowupAlertRequestDto request,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(commandPort.updateAlert(id, UUID.fromString(authentication.getName()), request)));
    }

    @PostMapping("/adoptions/{id}/support-request")
    @Operation(summary = "I need support", description = "The adopter, any time after the adoption completed. Opens yellow work for the organization.")
    public ResponseEntity<ApiResponse<Void>> supportRequest(@PathVariable UUID id,
                                                            @Valid @RequestBody(required = false) SupportRequestDto request,
                                                            Authentication authentication) {
        commandPort.requestSupport(id, UUID.fromString(authentication.getName()), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/adoptions/{id}/return-request")
    @Operation(summary = "I cannot keep the pet any more",
            description = "The adopter. Opens red work and tells the organization now. Costs no reputation.")
    public ResponseEntity<ApiResponse<Void>> returnRequest(@PathVariable UUID id,
                                                           @Valid @RequestBody ReturnRequestDto request,
                                                           Authentication authentication) {
        commandPort.requestReturn(id, UUID.fromString(authentication.getName()), request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/adoptions/{id}/followup-preference")
    @Operation(summary = "Turn the follow-up questions off or on", description = "The adopter.")
    public ResponseEntity<ApiResponse<Void>> preference(@PathVariable UUID id,
                                                        @Valid @RequestBody FollowupPreferenceDto request,
                                                        Authentication authentication) {
        commandPort.setPreference(id, UUID.fromString(authentication.getName()), request.enabled());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/adoptions/{id}/outcome")
    @Operation(summary = "Record how an adoption ended",
            description = "Owners and staff of the organization that handed the pet over, and admins. RETURNED_BY_ADOPTER "
                    + "brings the pet back to the organization (FOSTERING) without touching the adopter's reputation.")
    public ResponseEntity<ApiResponse<AdoptionOutcomeDto>> outcome(@PathVariable UUID id,
                                                                   @Valid @RequestBody RecordOutcomeRequestDto request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(commandPort.recordOutcome(id, UUID.fromString(authentication.getName()), request)));
    }

    @GetMapping("/adoptions/{id}/followups")
    @Operation(summary = "Timeline of follow-up for an adoption",
            description = "The adopter, the organization that handed the pet over, and admins.")
    public ResponseEntity<ApiResponse<List<FollowupDto>>> timeline(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.timeline(id, UUID.fromString(authentication.getName()))));
    }
}
