package com.uit.petrescueapi.application.dto.pet;

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
    private UUID mediaFileId;
    private boolean primary;
}
