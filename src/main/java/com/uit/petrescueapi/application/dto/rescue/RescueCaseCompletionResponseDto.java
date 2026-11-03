package com.uit.petrescueapi.application.dto.rescue;

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
public class RescueCaseCompletionResponseDto {
    private UUID completionId;
    private UUID caseId;
    private LocalDateTime rescuedAt;
    private String rescueNote;
    private String locationNote;
    private UUID verifiedBy;
    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private List<String> verificationImagesUrl;
    private UUID submittedBy;
    private String submittedByName;
    private String outcome;
    private String status;
    private String rejectReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerificationImageDto {
        private UUID mediaId;
        private String url;
    }
}
