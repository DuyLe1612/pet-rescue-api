package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapped to the {@code adoption_applications} table.
 */
@Entity
@Table(name = "adoption_applications", indexes = {
        @Index(name = "idx_adoption_pet", columnList = "pet_id"),
        @Index(name = "idx_adoption_applicant", columnList = "applicant_id"),
        @Index(name = "idx_adoption_status", columnList = "status")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdoptionApplicationJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "application_id", updatable = false, nullable = false)
    private UUID applicationId;

    @Column(name = "adoption_code", nullable = false, updatable = false)
    private String adoptionCode;

    @Column(name = "pet_id")
    private UUID petId;

    @Column(name = "applicant_id")
    private UUID applicantId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "experience", columnDefinition = "TEXT")
    private String experience;

    @Column(name = "live_condition", columnDefinition = "TEXT")
    private String liveCondition;

    @Column(name = "decided_at", columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime decidedAt;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @Column(name = "ready_at", columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime readyAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private PetJpaEntity pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", insertable = false, updatable = false)
    private UserJpaEntity applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", insertable = false, updatable = false)
    private OrganizationJpaEntity organization;
}
