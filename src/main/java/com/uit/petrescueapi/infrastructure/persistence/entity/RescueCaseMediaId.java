package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RescueCaseMediaId implements Serializable {

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "media_id")
    private UUID mediaId;
}
