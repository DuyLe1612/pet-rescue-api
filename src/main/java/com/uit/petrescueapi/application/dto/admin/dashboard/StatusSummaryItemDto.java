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
public class StatusSummaryItemDto implements Serializable {
    private String status;
    private String label;
    private long count;
}
