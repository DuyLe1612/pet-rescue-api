package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.Banner;
import com.uit.petrescueapi.domain.repository.BannerRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.BannerEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.BannerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BannerRepositoryAdapter implements BannerRepository {

    private final BannerJpaRepository jpa;
    private final BannerEntityMapper mapper;

    @Override
    public Banner save(Banner banner) {
        return mapper.toDomain(jpa.save(mapper.toEntity(banner)));
    }

    @Override
    public Optional<Banner> findById(UUID bannerId) {
        return jpa.findById(bannerId)
                .filter(e -> !e.isDeleted())
                .map(mapper::toDomain);
    }

    @Override
    public Page<Banner> findAll(Pageable pageable) {
        return jpa.findAllFiltered(null, null, null, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Banner> findAllFiltered(String targetPage, Boolean active, String search, Pageable pageable) {
        if (targetPage != null && active != null) {
            return jpa.findAllFiltered(targetPage, active, search, pageable).map(mapper::toDomain);
        }
        if (targetPage != null) {
            return jpa.findAllFiltered(targetPage, null, search, pageable).map(mapper::toDomain);
        }
        if (active != null) {
            return jpa.findAllFiltered(null, active, search, pageable).map(mapper::toDomain);
        }
        return jpa.findAllFiltered(null, null, search, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Banner> findByTargetPage(String targetPage, Pageable pageable) {
        return jpa.findAllFiltered(targetPage, null, null, pageable).map(mapper::toDomain);
    }

    @Override
    public List<Banner> findActiveByTargetPage(String targetPage) {
        return mapper.toDomainList(jpa.findActiveByTargetPage(targetPage, LocalDateTime.now()));
    }

    @Override
    public void delete(UUID bannerId) {
        jpa.deleteById(bannerId);
    }
}
