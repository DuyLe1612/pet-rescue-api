package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.banner.BannerResponseDto;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.query.BannerQueryPort;
import com.uit.petrescueapi.domain.entity.Banner;
import com.uit.petrescueapi.domain.service.BannerDomainService;
import com.uit.petrescueapi.domain.service.MediaFileDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Query (read) use-case for Banner operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BannerQueryUseCase implements BannerQueryPort {

    private final BannerDomainService domainService;
    private final MediaFileDomainService mediaService;
    private final CloudStoragePort cloudStoragePort;

    @Override
    public BannerResponseDto findById(UUID bannerId) {
        log.debug("Query: find banner by id {}", bannerId);
        return toDto(domainService.findById(bannerId));
    }

    @Override
    public Page<BannerResponseDto> findAll(Pageable pageable) {
        log.debug("Query: find all banners");
        return domainService.findAll(pageable).map(this::toDto);
    }

    @Override
    public Page<BannerResponseDto> findAllFiltered(String targetPage, Boolean active, String search, Pageable pageable) {
        log.debug("Query: find banners with filters targetPage={}, active={}, search={}", targetPage, active, search);
        return domainService.findAllFiltered(targetPage, active, search, pageable).map(this::toDto);
    }

    @Override
    public Page<BannerResponseDto> findByTargetPage(String targetPage, Pageable pageable) {
        log.debug("Query: find banners by target page {}", targetPage);
        return domainService.findByTargetPage(targetPage, pageable).map(this::toDto);
    }

    @Override
    public List<BannerResponseDto> findActiveByTargetPage(String targetPage) {
        log.debug("Query: find active banners by target page {}", targetPage);
        return domainService.findActiveByTargetPage(targetPage)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private BannerResponseDto toDto(Banner banner) {
        String mediaUrl = null;
        if (banner.getMediaId() != null) {
            try {
                var media = mediaService.findById(banner.getMediaId());
                mediaUrl = cloudStoragePort.buildUrl(media.getPublicId());
            } catch (Exception e) {
                log.warn("Failed to resolve media URL for banner {}: {}", banner.getBannerId(), e.getMessage());
            }
        }

        return BannerResponseDto.builder()
                .bannerId(banner.getBannerId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .buttonText(banner.getButtonText())
                .mediaId(banner.getMediaId())
                .mediaUrl(mediaUrl)
                .linkUrl(banner.getLinkUrl())
                .linkType(banner.getLinkType())
                .displayOrder(banner.getDisplayOrder())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .active(Boolean.TRUE.equals(banner.getActive()))
                .targetPage(banner.getTargetPage())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }
}
