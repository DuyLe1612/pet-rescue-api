package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.banner.CreateBannerRequestDto;
import com.uit.petrescueapi.application.dto.banner.UpdateBannerRequestDto;
import com.uit.petrescueapi.application.port.command.BannerCommandPort;
import com.uit.petrescueapi.application.port.command.MediaCommandPort;
import com.uit.petrescueapi.domain.entity.Banner;
import com.uit.petrescueapi.domain.service.BannerDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Command (write) use-case for Banner operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BannerCommandUseCase implements BannerCommandPort {

    private final BannerDomainService domainService;
    private final MediaCommandPort mediaCommandPort;

    @Override
    public Banner create(CreateBannerRequestDto cmd) {
        log.debug("Command: create banner with title {}", cmd.getTitle());
        UUID bannerId = UUID.randomUUID();
        UUID mediaId = confirmBannerMedia(cmd.getMediaId(), bannerId);
        Banner banner = Banner.builder()
                .bannerId(bannerId)
                .title(cmd.getTitle())
                .subtitle(cmd.getSubtitle())
                .buttonText(cmd.getButtonText())
                .mediaId(mediaId)
                .linkUrl(cmd.getLinkUrl())
                .linkType(cmd.getLinkType())
                .displayOrder(cmd.getDisplayOrder())
                .startDate(cmd.getStartDate())
                .endDate(cmd.getEndDate())
                .active(cmd.isActive())
                .targetPage(cmd.getTargetPage())
                .build();
        return domainService.create(banner);
    }

    @Override
    public Banner update(UUID bannerId, UpdateBannerRequestDto cmd) {
        log.debug("Command: update banner {}", bannerId);
        UUID mediaId = confirmBannerMedia(cmd.getMediaId(), bannerId);
        Banner updated = Banner.builder()
                .title(cmd.getTitle())
                .subtitle(cmd.getSubtitle())
                .buttonText(cmd.getButtonText())
            .mediaId(mediaId)
                .linkUrl(cmd.getLinkUrl())
                .linkType(cmd.getLinkType())
                .displayOrder(cmd.getDisplayOrder())
                .startDate(cmd.getStartDate())
                .endDate(cmd.getEndDate())
                .active(cmd.getActive())
                .targetPage(cmd.getTargetPage())
                .build();
        return domainService.update(bannerId, updated);
    }

    @Override
    public Banner toggleActive(UUID bannerId) {
        log.debug("Command: toggle active for banner {}", bannerId);
        return domainService.toggleActive(bannerId);
    }

    @Override
    public Banner updateDisplayOrder(UUID bannerId, Integer displayOrder) {
        log.debug("Command: update display order for banner {} to {}", bannerId, displayOrder);
        return domainService.updateDisplayOrder(bannerId, displayOrder);
    }

    private UUID confirmBannerMedia(UUID mediaId, UUID bannerId) {
        if (mediaId == null) {
            return null;
        }

        try {
            mediaCommandPort.confirmUpload(mediaId, "banners/" + bannerId);
        } catch (RuntimeException ignored) {
            // Already permanent or otherwise not confirmable; keep the existing media reference.
        }

        return mediaId;
    }

    @Override
    public void delete(UUID bannerId) {
        log.debug("Command: delete banner {}", bannerId);
        domainService.delete(bannerId);
    }
}
