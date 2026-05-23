package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.petrescueapi.application.dto.geo.ProvinceDetailDto;
import com.uit.petrescueapi.application.dto.geo.ProvinceSummaryDto;
import com.uit.petrescueapi.application.dto.geo.WardDto;
import com.uit.petrescueapi.application.port.out.ProvinceQueryDataPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProvinceQueryAdapter implements ProvinceQueryDataPort {

    private static final String ADDRESS_RESOURCE = "templates/addresssystem.json";

    private final ObjectMapper objectMapper;

    @Override
    public Page<ProvinceSummaryDto> listProvinces(String search, int page, int size) {
        List<ProvinceDetailDto> all = loadAll();
        List<ProvinceSummaryDto> filtered = new ArrayList<>();
        String keyword = search == null ? "" : search.toLowerCase(Locale.ROOT).trim();

        for (ProvinceDetailDto item : all) {
            if (keyword.isEmpty() || (item.getName() != null && item.getName().toLowerCase(Locale.ROOT).contains(keyword))) {
                filtered.add(toSummary(item));
            }
        }

        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        List<ProvinceSummaryDto> pageItems = filtered.subList(start, end);
        return new PageImpl<>(pageItems, PageRequest.of(page, size), filtered.size());
    }

    @Override
    public ProvinceDetailDto getProvinceDetail(int code, Integer depth) {
        List<ProvinceDetailDto> all = loadAll();
        ProvinceDetailDto found = all.stream()
                .filter(p -> p.getCode() != null && p.getCode() == code)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Province", "code", code));

        if (depth == null || depth < 2) {
            found.setWards(List.of());
        }

        return found;
    }

    private List<ProvinceDetailDto> loadAll() {
        try (InputStream input = new ClassPathResource(ADDRESS_RESOURCE).getInputStream()) {
            List<Map<String, Object>> raw = objectMapper.readValue(input, new TypeReference<>() {});
            return raw.stream().map(this::toDetail).toList();
        } catch (IOException e) {
            log.error("Failed to load province data", e);
            return List.of();
        }
    }

    private ProvinceDetailDto toDetail(Map<String, Object> raw) {
        List<WardDto> wards = new ArrayList<>();
        Object wardObj = raw.get("wards");
        if (wardObj instanceof List<?> wardList) {
            for (Object item : wardList) {
                if (item instanceof Map<?, ?> w) {
                    wards.add(WardDto.builder()
                            .code(asInt(w.get("code")))
                            .name(asString(w.get("name")))
                            .codename(asString(w.get("codename")))
                            .divisionType(asString(w.get("division_type")))
                            .provinceCode(asInt(w.get("province_code")))
                            .build());
                }
            }
        }

        return ProvinceDetailDto.builder()
                .code(asInt(raw.get("code")))
                .name(asString(raw.get("name")))
                .codename(asString(raw.get("codename")))
                .divisionType(asString(raw.get("division_type")))
                .phoneCode(asInt(raw.get("phone_code")))
                .wards(wards)
                .build();
    }

    private ProvinceSummaryDto toSummary(ProvinceDetailDto dto) {
        return ProvinceSummaryDto.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .codename(dto.getCodename())
                .divisionType(dto.getDivisionType())
                .phoneCode(dto.getPhoneCode())
                .build();
    }

    private Integer asInt(Object value) {
        if (value == null) return null;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        return Integer.valueOf(value.toString());
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
