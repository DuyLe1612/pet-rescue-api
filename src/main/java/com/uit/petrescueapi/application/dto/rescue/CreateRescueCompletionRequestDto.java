package com.uit.petrescueapi.application.dto.rescue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRescueCompletionRequestDto {

    private UUID caseId;

    @NotEmpty(message = "At least one verification image is required")
    private List<UUID> verificationImageIds;

    private String rescueNote;
    @Schema(description = "Local Date Time pls include time", example = "")
    private LocalDateTime rescuedAt;

    private String locationNote;

    @AssertTrue(message = "You must confirm rescue completion")
    private Boolean confirmRescued;
}

