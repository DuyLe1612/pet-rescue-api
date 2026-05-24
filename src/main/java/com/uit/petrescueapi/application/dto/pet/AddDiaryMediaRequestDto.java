package com.uit.petrescueapi.application.dto.pet;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Request DTO for adding a media entry to pet diary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddDiaryMediaRequestDto {

    @NotNull
    private UUID mediaId;
}
