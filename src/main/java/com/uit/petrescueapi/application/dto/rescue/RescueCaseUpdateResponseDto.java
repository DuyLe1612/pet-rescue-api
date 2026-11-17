package com.uit.petrescueapi.application.dto.rescue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** One progress note on a rescue case. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescueCaseUpdateResponseDto {
    private UUID updateId;
    private UUID caseId;
    private UUID authorId;
    private String authorUsername;
    private String authorAvatarUrl;
    private String note;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}
