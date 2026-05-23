package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.geo.ProvinceDetailDto;
import com.uit.petrescueapi.application.dto.geo.ProvinceSummaryDto;
import com.uit.petrescueapi.application.port.out.ProvinceQueryDataPort;
import com.uit.petrescueapi.application.port.query.ProvinceQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProvinceQueryUseCase implements ProvinceQueryPort {

    private final ProvinceQueryDataPort queryDataPort;

    @Override
    public Page<ProvinceSummaryDto> listProvinces(String search, int page, int size) {
        log.debug("Query: list provinces page {} size {}", page, size);
        return queryDataPort.listProvinces(search, page, size);
    }

    @Override
    public ProvinceDetailDto getProvinceDetail(int code, Integer depth) {
        log.debug("Query: get province detail {} depth {}", code, depth);
        return queryDataPort.getProvinceDetail(code, depth);
    }
}
