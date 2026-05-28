package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.Banner;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain service encapsulating Banner business rules.
 *
 * Rules:
 *  - New banners get a generated id and createdAt timestamp.
 *  - Default displayOrder is 0 if not specified.
 *  - Deletion is soft-delete (sets deleted = true).
 *
 * {@code @Transactional} lives here only — not on adapters or controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerDomainService {

    private final BannerRepository bannerRepository;

    // ── Queries ─────────────────────────────────────

    @Transactional(readOnly = true)
    public Banner findById(UUID bannerId) {
        return bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Banner", "bannerId", bannerId));
    }

    @Transactional(readOnly = true)
    public Page<Banner> findAll(Pageable pageable) {
        return bannerRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Banner> findAllFiltered(String targetPage, Boolean active, Pageable pageable) {
        return bannerRepository.findAllFiltered(targetPage, active, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Banner> findByTargetPage(String targetPage, Pageable pageable) {
        return bannerRepository.findByTargetPage(targetPage, pageable);
    }

    @Transactional(readOnly = true)
    public List<Banner> findActiveByTargetPage(String targetPage) {
        return bannerRepository.findActiveByTargetPage(targetPage);
    }

    // ── Commands ────────────────────────────────────

    /**
     * Create a new banner.
     * Sets the id, createdAt, and default values.
     */
    public Banner create(Banner banner) {
        log.info("Creating banner: {}", banner.getTitle());
        if (banner.getBannerId() == null) {
            banner.setBannerId(UUID.randomUUID());
        }
        banner.setCreatedAt(LocalDateTime.now());
        
        // Set defaults
        if (banner.getDisplayOrder() == null) {
            banner.setDisplayOrder(0);
        }
        if (banner.getLinkType() == null) {
            banner.setLinkType("NONE");
        }
        if (banner.getTargetPage() == null) {
            banner.setTargetPage("HOME");
        }
        if (banner.getActive() == null) {
            banner.setActive(true);
        }

        return bannerRepository.save(banner);
    }

    /**
     * Update an existing banner.
     */
    public Banner update(UUID bannerId, Banner updated) {
        log.info("Updating banner: {}", bannerId);
        Banner existing = findById(bannerId);
        applyUpdates(existing, updated);
        existing.setUpdatedAt(LocalDateTime.now());
        return bannerRepository.save(existing);
    }

    /**
     * Toggle banner active status.
     */
    public Banner toggleActive(UUID bannerId) {
        log.info("Toggling active status for banner: {}", bannerId);
        Banner banner = findById(bannerId);
        banner.setActive(!Boolean.TRUE.equals(banner.getActive()));
        banner.setUpdatedAt(LocalDateTime.now());
        return bannerRepository.save(banner);
    }

    /**
     * Update display order.
     */
    public Banner updateDisplayOrder(UUID bannerId, Integer displayOrder) {
        log.info("Updating display order for banner {} to {}", bannerId, displayOrder);
        Banner banner = findById(bannerId);
        banner.setDisplayOrder(displayOrder);
        banner.setUpdatedAt(LocalDateTime.now());
        return bannerRepository.save(banner);
    }

    /**
     * Soft delete a banner.
     */
    public void delete(UUID bannerId) {
        Banner banner = findById(bannerId);
        banner.setDeleted(true);
        banner.setDeletedAt(LocalDateTime.now());
        bannerRepository.save(banner);
        log.info("Soft-deleted banner {}", bannerId);
    }

    // ── Private helpers ─────────────────────────────

    private void applyUpdates(Banner target, Banner source) {
        if (source.getTitle() != null) target.setTitle(source.getTitle());
        if (source.getSubtitle() != null) target.setSubtitle(source.getSubtitle());
        if (source.getButtonText() != null) target.setButtonText(source.getButtonText());
        if (source.getMediaId() != null) target.setMediaId(source.getMediaId());
        if (source.getLinkUrl() != null) target.setLinkUrl(source.getLinkUrl());
        if (source.getLinkType() != null) target.setLinkType(source.getLinkType());
        if (source.getDisplayOrder() != null) target.setDisplayOrder(source.getDisplayOrder());
        if (source.getStartDate() != null) target.setStartDate(source.getStartDate());
        if (source.getEndDate() != null) target.setEndDate(source.getEndDate());
        if (source.getActive() != null) target.setActive(source.getActive());
        if (source.getTargetPage() != null) target.setTargetPage(source.getTargetPage());
    }
}
