package com.uit.petrescueapi.application.dto.pet;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachPetMediaRequestDto {
    @NotNull
    private UUID mediaFileId;
    private boolean primary;
}
