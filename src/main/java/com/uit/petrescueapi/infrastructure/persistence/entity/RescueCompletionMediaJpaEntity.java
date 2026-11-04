package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rescue_completion_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescueCompletionMediaJpaEntity {

    @EmbeddedId
    private RescueCompletionMediaId id;
}