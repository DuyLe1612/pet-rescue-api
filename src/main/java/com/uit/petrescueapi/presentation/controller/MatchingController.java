package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.matching.AdopterProfileDto;
import com.uit.petrescueapi.application.dto.matching.MatchResultDto;
import com.uit.petrescueapi.application.dto.matching.PetTraitsDto;
import com.uit.petrescueapi.application.port.command.MatchingCommandPort;
import com.uit.petrescueapi.application.port.query.MatchingQueryPort;
import com.uit.petrescueapi.domain.exception.UnauthorizedException;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Matching", description = "Explainable compatibility between adopters and pets")
public class MatchingController {

    private final MatchingQueryPort queryPort;
    private final MatchingCommandPort commandPort;

    @GetMapping("/users/me/adopter-profile")
    @Operation(summary = "My adopter profile", description = "404 until it has been saved once.")
    public ResponseEntity<ApiResponse<AdopterProfileDto>> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.getAdopterProfile(userId(authentication))));
    }

    @PutMapping("/users/me/adopter-profile")
    @Operation(summary = "Create or replace my adopter profile",
            description = "consent must be true: organizations you apply to can read this profile.")
    public ResponseEntity<ApiResponse<AdopterProfileDto>> saveMyProfile(
            @Valid @RequestBody AdopterProfileDto profile, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(commandPort.saveAdopterProfile(userId(authentication), profile)));
    }

    @GetMapping("/pets/{petId}/traits")
    @Operation(summary = "What is recorded about a pet for matching",
            description = "Public. Fields nobody recorded are absent.")
    public ResponseEntity<ApiResponse<PetTraitsDto>> getTraits(@PathVariable UUID petId) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.getPetTraits(petId)));
    }

    @PutMapping("/pets/{petId}/traits")
    @Operation(summary = "Record what is known about a pet",
            description = "For whoever manages the pet: its owner, the organization's OWNER, STAFF or VET, or an admin. "
                    + "Replaces the whole record.")
    public ResponseEntity<ApiResponse<PetTraitsDto>> saveTraits(
            @PathVariable UUID petId, @Valid @RequestBody PetTraitsDto traits, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(commandPort.savePetTraits(petId, traits, userId(authentication))));
    }

    @GetMapping("/pets/{petId}/match")
    @Operation(summary = "How well this pet fits me",
            description = "Level, certainty, a reason per criterion, warnings and what to prepare. "
                    + "Needs a saved adopter profile (422 ADOPTER_PROFILE_REQUIRED otherwise).")
    public ResponseEntity<ApiResponse<MatchResultDto>> match(@PathVariable UUID petId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.matchForPet(petId, userId(authentication))));
    }

    /** GET /pets/** is open to anonymous callers, so the caller can be missing here. */
    private static UUID userId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Sign in to use matching");
        }
        return UUID.fromString(authentication.getName());
    }
}
