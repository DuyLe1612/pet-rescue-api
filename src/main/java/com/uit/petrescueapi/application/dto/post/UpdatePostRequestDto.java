package com.uit.petrescueapi.application.dto.post;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating a post.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequestDto {

    @Size(max = 2000)
    private String content;

    @Size(max = 10)
    private List<UUID> mediaIds;

    @Size(max = 20)
    private List<String> tagCodes;

    /** Link the post to this rescue case, replacing any current link. */
    private java.util.UUID rescueCaseId;

    /** True removes the link to a rescue case. Ignored when rescueCaseId is sent. */
    private boolean removeRescueCase;
}
