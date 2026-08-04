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
public class KpiItemDto implements Serializable {
    private long value;
    private double changePercent;
    private String trend; // "up", "down", "neutral"
}
