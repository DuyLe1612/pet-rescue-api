package com.uit.petrescueapi.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Post — represents a community post related to rescues.
 * Extends BaseEntity for audit fields.
 * Pure domain entity: no JPA annotations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Post extends BaseEntity {

    private UUID postId;
    private UUID authorId;
    private UUID rescueCaseId;
    private String content;

    @Builder.Default
    private List<UUID> mediaIds = new ArrayList<>();

    @Builder.Default
    private List<UUID> tagIds = new ArrayList<>();

    /** Tag codes as sent by the client; {@code null} on a patch means "unchanged". */
    private List<String> tagCodes;
}
