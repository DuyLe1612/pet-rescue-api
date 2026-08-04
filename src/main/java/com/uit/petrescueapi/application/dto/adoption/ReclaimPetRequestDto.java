package com.uit.petrescueapi.application.dto.adoption;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReclaimPetRequestDto {
    @NotBlank
    @Size(min = 10, max = 2000)
    private String reason;

    @NotNull
    @JsonAlias("proof_id")
    private UUID proofId;
}
