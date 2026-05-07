package com.uit.petrescueapi.application.dto.adoption;

import lombok.*;

/**
 * Request DTO for adoption approval/rejection decision.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRequestDto {

    private String note;
    private String rejectReason;
    private java.time.LocalDateTime readyAt;
}
