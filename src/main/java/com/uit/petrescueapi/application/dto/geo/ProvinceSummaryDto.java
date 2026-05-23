package com.uit.petrescueapi.application.dto.geo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceSummaryDto {
    private Integer code;
    private String name;
    private String codename;
    private String divisionType;
    private Integer phoneCode;
}
