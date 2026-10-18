package com.uit.petrescueapi.infrastructure.persistence.mongo.document;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.infrastructure.persistence.mongo.MongoCollections;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One model call for one post (DA2 Goal 4 dataset): model output next to the label the author
 * later confirmed, so accuracy per prompt version is a single aggregation. Never expires.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(MongoCollections.POST_ANALYSIS_RUNS)
public class PostAnalysisRunDocument {
    @Id
    private UUID id;
    private UUID postId;
    private String model;
    private int promptVersion;
    private int schemaVersion;
    private String rawOutput;
    /** Model output after backend validation; null when the call failed. */
    private PostAnalysisSuggestion parsed;
    private String failure;
    @Builder.Default
    private List<String> validationErrors = new ArrayList<>();
    private long latencyMs;
    private long inputTokens;
    private long outputTokens;
    private LocalDateTime createdAt;
    /** Copied from the confirmed facets; ground truth for accuracy. */
    private HumanLabelDocument humanLabel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HumanLabelDocument {
        private String category;
        private String species;
        private String district;
        private String province;
        private boolean needsRescue;
    }
}
