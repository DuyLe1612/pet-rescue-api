package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for the Banner entity.
 */
public interface BannerRepository {

    Banner save(Banner banner);

    Optional<Banner> findById(UUID bannerId);

    Page<Banner> findAll(Pageable pageable);

    Page<Banner> findAllFiltered(String targetPage, Boolean active, String search, Pageable pageable);

    Page<Banner> findByTargetPage(String targetPage, Pageable pageable);

    List<Banner> findActiveByTargetPage(String targetPage);

    void delete(UUID bannerId);
}
