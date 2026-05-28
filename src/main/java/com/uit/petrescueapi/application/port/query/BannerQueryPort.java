package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.banner.BannerResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Query (read) port for Banner operations.
 */
public interface BannerQueryPort {

    BannerResponseDto findById(UUID bannerId);

    Page<BannerResponseDto> findAll(Pageable pageable);

    Page<BannerResponseDto> findAllFiltered(String targetPage, Boolean active, String search, Pageable pageable);

    Page<BannerResponseDto> findByTargetPage(String targetPage, Pageable pageable);

    /**
     * Find active banners for a specific page (within date range, sorted by display order).
     * This is the primary endpoint for public banner display.
     */
    List<BannerResponseDto> findActiveByTargetPage(String targetPage);
}
