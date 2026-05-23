package com.uit.petrescueapi.presentation.controller;

import com.uit.petrescueapi.application.dto.geo.ProvinceDetailDto;
import com.uit.petrescueapi.application.dto.geo.ProvinceSummaryDto;
import com.uit.petrescueapi.application.port.query.ProvinceQueryPort;
import com.uit.petrescueapi.presentation.dto.ApiResponse;
import com.uit.petrescueapi.presentation.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name = "Locations", description = "Province and ward queries")
public class LocationController {

    private final ProvinceQueryPort queryPort;

    @GetMapping("/p")
    @Operation(summary = "List provinces")
    public ResponseEntity<ApiResponse<PageResponse<ProvinceSummaryDto>>> listProvinces(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Page<ProvinceSummaryDto> result = queryPort.listProvinces(search, page, size);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/p/{code}")
    @Operation(summary = "Get province detail")
    public ResponseEntity<ApiResponse<ProvinceDetailDto>> getProvinceDetail(
            @PathVariable int code,
            @RequestParam(required = false, defaultValue = "2") Integer depth
    ) {
        return ResponseEntity.ok(ApiResponse.ok(queryPort.getProvinceDetail(code, depth)));
    }
}
