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
public class RescueCompletionMediaId implements Serializable {

    @Column(name = "completion_id")
    private UUID completionId;

    @Column(name = "media_id")
    private UUID mediaId;
}
