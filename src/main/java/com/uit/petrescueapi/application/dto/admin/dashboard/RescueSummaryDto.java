package com.uit.petrescueapi.application.dto.admin.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescueSummaryDto implements Serializable {
    private PeriodDto period;
    private List<StatusSummaryItemDto> byStatus;
    private List<PrioritySummaryItemDto> byPriority;
}
