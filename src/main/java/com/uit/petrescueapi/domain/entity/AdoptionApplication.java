package com.uit.petrescueapi.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AdoptionApplication — represents an adoption request from a user.
 * Extends BaseEntity for audit fields.
 * Pure domain entity: no JPA annotations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdoptionApplication extends BaseEntity {

    private UUID applicationId;
    private String adoptionCode;
    private UUID petId;
    private UUID applicantId;
    private UUID organizationId;
    private String status;
    private String note;
    private String experience;
    private String liveCondition;
    private LocalDateTime decidedAt;
    private UUID decidedBy;
    private String rejectReason;
    private LocalDateTime readyAt;
}
