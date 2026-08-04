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
public class AdoptionTrendItemDto implements Serializable {
    private int month;
    private String label;
    private long pending;
    private long approved;
    private long rejected;
    private long completed;
}
