package com.uit.petrescueapi.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * RescueCase Completions — represents an animal rescue case which have been rescued by someone.
 * Extends BaseEntity for audit fields.
 * Pure domain entity: no JPA annotations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(callSuper = true)
public class RescueCaseCompletion extends BaseEntity {

    private UUID completionId;

    private UUID caseId;

    @Builder.Default
    private List<UUID> mediaId = new ArrayList<>();

    private LocalDateTime rescuedAt;

    private String rescueNote;

    private String locationNote;

    private UUID verifiedBy;

    private LocalDateTime verifiedAt;

}

