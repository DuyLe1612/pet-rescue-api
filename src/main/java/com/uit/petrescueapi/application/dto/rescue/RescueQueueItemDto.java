package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row of an organization's rescue queue. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescueQueueItemDto {

    private UUID caseId;
    private String caseCode;
    private String species;
    private RescuePriority priority;
    private String status;
    private String description;
    private String provinceName;
    private String wardName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime reportedAt;
    private boolean needsHelp;
    /** Already rescued by someone who wants to hand the animal to an organization. */
    private boolean awaitingHandover;
    private long activeClaimCount;
}
