package com.uit.petrescueapi.application.dto.analysis;

import com.uit.petrescueapi.domain.valueobject.PostAnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI analysis of a post: suggestion (AI) vs facets (confirmed by the author)")
public class PostAnalysisResponseDto {
    private UUID postId;
    private PostAnalysisStatus status;
    private String model;
    private LocalDateTime analyzedAt;
    private PostAnalysisSuggestion suggestion;
    private PostFacets facets;
    @Schema(description = "NONE | SUGGESTED | ACCEPTED")
    private String rescueSuggestion;
    private UUID rescueCaseId;
}
