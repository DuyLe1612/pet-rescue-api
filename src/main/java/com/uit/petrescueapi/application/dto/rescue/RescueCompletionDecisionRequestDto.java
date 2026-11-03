package com.uit.petrescueapi.application.dto.rescue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescueCompletionDecisionRequestDto {

    @NotBlank(message = "A reason is required")
    @Size(max = 1000)
    private String reason;
}
