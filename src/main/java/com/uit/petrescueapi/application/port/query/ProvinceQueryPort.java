package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.location.ProvinceDetailDto;
import com.uit.petrescueapi.application.dto.location.ProvinceSummaryDto;
import org.springframework.data.domain.Page;

public interface ProvinceQueryPort {
    Page<ProvinceSummaryDto> listProvinces(String search, int page, int size);

    ProvinceDetailDto getProvinceDetail(int code, Integer depth);
}
