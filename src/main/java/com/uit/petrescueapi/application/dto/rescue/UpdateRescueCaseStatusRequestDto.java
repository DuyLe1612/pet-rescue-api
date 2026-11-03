package com.uit.petrescueapi.application.dto.rescue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Request DTO for updating rescue case status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRescueCaseStatusRequestDto {

    @NotBlank
    @Pattern(regexp = "^(REPORTED|IN_PROGRESS|RESCUED|CLOSED)$")
    private String status;  // REPORTED | IN_PROGRESS | RESCUED | CLOSED

    @jakarta.validation.constraints.Size(max = 1000)
    @io.swagger.v3.oas.annotations.media.Schema(description = "Why the case is closed. Required when closing, except for admins.")
    private String reason;
}
