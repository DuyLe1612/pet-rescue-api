package com.uit.petrescueapi.application.dto.adoption;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Request DTO for adoption approval/rejection decision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRequestDto {

    @Size(max = 2000)
    private String note;
    @Size(max = 2000)
    private String rejectReason;
    private java.time.LocalDateTime readyAt;

    /** Optional machine-readable reason for the decision, e.g. MATCH_WARNING, BETTER_FIT_FOUND, HOME_CHECK_OK. */
    @jakarta.validation.constraints.Size(max = 50)
    private String reasonCode;
}
