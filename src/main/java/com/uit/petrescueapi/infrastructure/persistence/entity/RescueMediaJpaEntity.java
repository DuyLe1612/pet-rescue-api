package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "rescue_media")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RescueCaseMediaId.class)
public class RescueMediaJpaEntity {

    @Id
    @Column(name = "case_id")
    private UUID caseId;

    @Id
    @Column(name = "media_id")
    private UUID mediaId;
}

