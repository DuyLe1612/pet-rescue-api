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

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("completionId")
    @JoinColumn(name = "completion_id")
    private RescueCaseCompletionJpaEntity completion;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mediaId")
    @JoinColumn(name = "media_id")
    private MediaFileJpaEntity media;
}