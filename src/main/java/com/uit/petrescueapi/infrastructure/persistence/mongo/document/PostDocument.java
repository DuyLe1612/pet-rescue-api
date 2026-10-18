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
 * Render-ready post (migrate.md §3.1): one read serves a feed card or the detail page —
 * author, image public ids, tags, counters, preview comments and the DA2 facets are embedded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(MongoCollections.POSTS)
public class PostDocument {

    public static final int PREVIEW_COMMENTS = 2;

    @Id
    private UUID id;
    private AuthorDocument author;
    private String content;
    @Builder.Default
    private List<MediaDocument> media = new ArrayList<>();
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    @Builder.Default
    private StatsDocument stats = new StatsDocument();
    @Builder.Default
    private List<CommentPreviewDocument> previewComments = new ArrayList<>();
    private RescueCaseRefDocument rescueCase;
    /** User-confirmed structured data (DA2 Goal 1); the only fields the feed filters on. */
    private FacetsDocument facets;
    /** AI analysis state + suggestion (never used for filtering until confirmed into {@link #facets}). */
    private AnalysisDocument analysis;
    /** NONE | SUGGESTED | ACCEPTED | DISMISSED */
    private String rescueSuggestion;
    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private UUID deletedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaDocument {
        private UUID mediaId;
        private String publicId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsDocument {
        private int likes;
        private int comments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentPreviewDocument {
        private UUID commentId;
        private AuthorDocument author;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RescueCaseRefDocument {
        private UUID caseId;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisDocument {
        /** {@link com.uit.petrescueapi.domain.valueobject.PostAnalysisStatus} name. */
        private String status;
        /** Version of the structured-output contract the suggestion follows. */
        private int schemaVersion;
        private String model;
        private LocalDateTime analyzedAt;
        private UUID lastRunId;
        private int attempts;
        private String lastError;
        private PostAnalysisSuggestion suggested;
        private UUID confirmedBy;
        private LocalDateTime confirmedAt;
        private UUID rejectedBy;
        private LocalDateTime updatedAt;

        public static AnalysisDocument pending(LocalDateTime now) {
            return AnalysisDocument.builder().status("PENDING").schemaVersion(SCHEMA_VERSION).updatedAt(now).build();
        }
    }

    public static final int SCHEMA_VERSION = 1;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacetsDocument {
        private String category;
        private String species;
        private String district;
        private String province;
        private Boolean needsRescue;
    }
}
