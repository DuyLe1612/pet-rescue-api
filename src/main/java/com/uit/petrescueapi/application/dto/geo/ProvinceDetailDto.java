package com.uit.petrescueapi.application.dto.geo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceDetailDto {
    private Integer code;
    private String name;
    private String codename;
    private String divisionType;
    private Integer phoneCode;
    private List<WardDto> wards;
}
