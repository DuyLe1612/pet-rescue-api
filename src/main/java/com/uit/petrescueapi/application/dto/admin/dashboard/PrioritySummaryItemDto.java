package com.uit.petrescueapi.application.dto.admin.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrioritySummaryItemDto implements Serializable {
    private String priority;
    private String label;
    private long count;
}
