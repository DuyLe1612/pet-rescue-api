package com.uit.petrescueapi.infrastructure.persistence.mongo.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** Last-message summary, embedded in {@code conversations} and {@code inboxes}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastMessageDocument {
    private Long seq;
    private UUID senderId;
    private String preview;
    private LocalDateTime at;
}
