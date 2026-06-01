package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rescue_case_completions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RescueCaseCompletionJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "completion_id", updatable = false, nullable = false)
    private UUID completionId;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "rescued_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime rescuedAt;

    @Column(name = "rescue_note", columnDefinition = "TEXT")
    private String rescueNote;

    @Column(name = "location_note", columnDefinition = "TEXT")
    private String locationNote;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    @Column(name = "verified_at", columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", insertable = false, updatable = false)
    private RescueCaseJpaEntity rescueCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by", insertable = false, updatable = false)
    private UserJpaEntity verifier;
}
