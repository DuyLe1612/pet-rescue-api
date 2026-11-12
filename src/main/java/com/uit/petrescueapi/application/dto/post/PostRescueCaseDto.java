package com.uit.petrescueapi.application.dto.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** The rescue case a post is about, with what a feed card needs so the client makes no second call. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Rescue case linked to a post")
public class PostRescueCaseDto {
    private UUID caseId;
    private String caseCode;
    @Schema(allowableValues = {"REPORTED", "IN_PROGRESS", "RESCUED", "CLOSED"})
    private String status;
    @Schema(description = "True while the case still needs help (REPORTED or IN_PROGRESS)")
    private boolean open;
    private String priority;
    private String species;
    private String locationText;
    private String provinceName;
    @Schema(description = "First image of the case")
    private String imageUrl;
}
