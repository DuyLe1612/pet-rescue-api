package com.uit.petrescueapi.application.dto.rescue;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** A case that may be the same animal as the one asked about, with why the rules think so. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseMatchResponseDto {

    private UUID suggestionId;
    @Schema(allowableValues = {"PENDING", "CONFIRMED", "REJECTED"})
    private String status;
    @Schema(description = "0-100 from distance, species, colour and marks; not a probability")
    private int score;
    @Schema(description = "Rounded to 100 m")
    private int distanceMeters;
    private List<String> reasons;
    private OtherCase otherCase;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherCase {
        private UUID caseId;
        private String caseCode;
        private String caseType;
        private String status;
        private String species;
        private String color;
        private String description;
        private String distinctiveMarks;
        private String holdingStatus;
        private Double latitude;
        private Double longitude;
        private Integer locationBlurMeters;
        private LocalDateTime reportedAt;
            /** The reporter's display name (full name, or "Thành viên PawHome"). Never the username. */
        private String reporterName;
        private String firstImageUrl;
    }
}
