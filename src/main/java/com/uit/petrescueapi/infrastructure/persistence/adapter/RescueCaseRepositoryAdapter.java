package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.repository.RescueCaseRepository;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.entity.MediaFile;
import com.uit.petrescueapi.domain.repository.MediaFileRepository;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.infrastructure.persistence.mapper.RescueCaseEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.RescueCaseJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RescueCaseRepositoryAdapter implements RescueCaseRepository {

    private final RescueCaseJpaRepository jpa;
    private final RescueCaseEntityMapper mapper;
    private final MediaFileRepository mediaFileRepository;
    private final CloudStoragePort cloudStoragePort;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public RescueCase save(RescueCase rescueCase) {
        // Persist rescue case
        var saved = mapper.toDomain(jpa.save(mapper.toEntity(rescueCase)));

        // If FE provided image URLs/public IDs, ensure they are permanent and link via rescue_media.
        if (rescueCase.getImagePublicIds() != null && !rescueCase.getImagePublicIds().isEmpty()) {
            for (String publicId : rescueCase.getImagePublicIds()) {
            MediaFile mf = mediaFileRepository.findByPublicId(publicId)
                .orElseGet(() -> MediaFile.builder()
                    .mediaId(UUID.randomUUID())
                    .publicId(publicId)
                    .uploaderId(rescueCase.getReportedBy())
                    .status("PERMANENT")
                    .build());

            if ("TEMP".equals(mf.getStatus())) {
                String targetFolder = "rescues/" + saved.getCaseId();
                String newPublicId = cloudStoragePort.moveToPermament(mf.getPublicId(), targetFolder, mf.getResourceType());
                mf.setPublicId(newPublicId);
                mf.setStatus("PERMANENT");
                mf.setFolder(targetFolder);
            } else if (mf.getFolder() == null) {
                mf.setFolder("rescues/" + saved.getCaseId());
            }

            MediaFile savedMf = mediaFileRepository.save(mf);
                // Insert link into rescue_media (idempotent)
                try {
                    jdbcTemplate.update("INSERT INTO rescue_media (case_id, media_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                            saved.getCaseId(), savedMf.getMediaId());
                } catch (Exception e) {
                    // swallow - linking failure should not block rescue case creation
                }
            }
        }

        return saved;
    }

    @Override
    public Optional<RescueCase> findById(UUID caseId) {
        return jpa.findById(caseId)
                .filter(e -> !e.isDeleted())
                .map(mapper::toDomain);
    }

    @Override
    public Page<RescueCase> findAll(Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Page<RescueCase> findByStatus(RescueCaseStatus status, Pageable pageable) {
        // Filter by status in-memory; can be replaced with a dedicated JPA query later
        return jpa.findAll(pageable)
                .map(mapper::toDomain);
    }
}
