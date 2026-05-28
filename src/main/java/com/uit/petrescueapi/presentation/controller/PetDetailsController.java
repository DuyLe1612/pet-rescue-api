package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.media.PetMediaResponseDto;
import com.uit.petrescueapi.application.dto.pet.*;
import com.uit.petrescueapi.application.port.command.PetDetailsCommandPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.application.port.query.PetDetailsQueryPort;
import com.uit.petrescueapi.domain.entity.PetMedicalRecord;
import com.uit.petrescueapi.domain.entity.PetMedia;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pets/{petId}")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Pet Details", description = "Pet sub-resource operations")
public class PetDetailsController {

    private final PetDetailsCommandPort commandPort;
    private final PetDetailsQueryPort queryPort;
        private final MediaQueryPort mediaQueryPort;

    @PostMapping("/medical-records")
    @Operation(summary = "Add a medical record for a pet")
    public ResponseEntity<ApiResponse<PetMedicalRecordResponseDto>> addMedicalRecord(
            @PathVariable UUID petId,
            @Valid @RequestBody CreateMedicalRecordRequestDto cmd) {
        PetMedicalRecord record = commandPort.addMedicalRecord(petId, cmd);
        PetMedicalRecordResponseDto dto = PetMedicalRecordResponseDto.builder()
                .recordId(record.getRecordId())
                .petId(record.getPetId())
                .description(record.getDescription())
                .vaccine(record.getVaccine())
                .diagnosis(record.getDiagnosis())
                .recordDate(record.getRecordDate())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(dto));
    }

    @GetMapping("/medical-records")
    @Operation(summary = "List medical records for a pet")
    public ResponseEntity<ApiResponse<PageResponse<PetMedicalRecordResponseDto>>> getMedicalRecords(
            @PathVariable UUID petId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findMedicalRecords(petId, PageRequest.of(page, size)))));
    }

    @GetMapping("/ownerships")
    @Operation(summary = "List ownership history for a pet")
        public ResponseEntity<ApiResponse<PageResponse<PetOwnershipHistoryDisplayDto>>> getOwnerships(
            @PathVariable UUID petId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findOwnerships(petId, PageRequest.of(page, size)))));
    }

    @GetMapping("/media")
    @Operation(summary = "List media attachments for a pet (paginated)",
               description = "Fetch all media files associated with a pet, paginated")
        public ResponseEntity<ApiResponse<PageResponse<PetMediaResponseDto>>> getMedia(
            @PathVariable UUID petId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(queryPort.findDiaryMedia(petId, PageRequest.of(page, size)))));
    }

        @PostMapping(value = "/media", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Upload a new image for a pet")
        public ResponseEntity<ApiResponse<PetMediaResponseDto>> addMedia(
                        @PathVariable UUID petId,
                        @RequestParam("file") MultipartFile file,
                        @RequestParam(value = "primary", defaultValue = "false") boolean primary,
                        Authentication authentication) {
                UUID requesterId = UUID.fromString(authentication.getName());
                PetMedia petMedia = commandPort.addMedia(petId, file, primary, requesterId);
                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toDto(petMedia)));
        }

        @PostMapping("/media/attach")
        @Operation(summary = "Attach an already uploaded image to a pet",
                           description = "Use this after a presigned/direct Cloudinary upload and /media/register flow.")
        public ResponseEntity<ApiResponse<PetMediaResponseDto>> attachMedia(
                        @PathVariable UUID petId,
                        @Valid @RequestBody AttachPetMediaRequestDto request,
                        Authentication authentication) {
                UUID requesterId = UUID.fromString(authentication.getName());
                PetMedia petMedia = commandPort.attachMedia(petId, request.getMediaFileId(), request.isPrimary(), requesterId);
                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toDto(petMedia)));
        }

        @PatchMapping("/media/{mediaId}/primary")
        @Operation(summary = "Set a pet image as primary")
        public ResponseEntity<ApiResponse<PetMediaResponseDto>> setPrimaryMedia(
                        @PathVariable UUID petId,
                        @PathVariable UUID mediaId,
                        Authentication authentication) {
                UUID requesterId = UUID.fromString(authentication.getName());
                PetMedia petMedia = commandPort.setPrimaryMedia(petId, mediaId, requesterId);
                return ResponseEntity.ok(ApiResponse.ok(toDto(petMedia)));
        }

        @DeleteMapping("/media/{mediaId}")
        @Operation(summary = "Delete a pet image")
        public ResponseEntity<ApiResponse<Void>> deleteMedia(
                        @PathVariable UUID petId,
                        @PathVariable UUID mediaId,
                        Authentication authentication) {
                UUID requesterId = UUID.fromString(authentication.getName());
                commandPort.deleteMedia(petId, mediaId, requesterId);
                return ResponseEntity.ok(ApiResponse.noContent());
        }

        private PetMediaResponseDto toDto(PetMedia petMedia) {
                String url = null;
                try {
                        url = mediaQueryPort.findById(petMedia.getMediaFileId()).getUrl();
                } catch (Exception ignored) {
                        // fall back to null when the media metadata is no longer available
                }
                return PetMediaResponseDto.builder()
                                .mediaId(petMedia.getMediaId())
                                .petId(petMedia.getPetId())
                                .mediaFileId(petMedia.getMediaFileId())
                                .url(url)
                                .type(petMedia.getType())
                                .primaryMedia(petMedia.isPrimaryMedia())
                                .createdAt(petMedia.getCreatedAt())
                                .build();
        }
}
