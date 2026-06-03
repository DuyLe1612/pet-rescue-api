package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.RescueMedia;
import com.uit.petrescueapi.domain.repository.RescueCaseMediaRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.RescueMediaEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.RescueMediaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RescueCaseMediaRepositoryAdapter
        implements RescueCaseMediaRepository {

    private final RescueMediaJpaRepository jpa;
    private final RescueMediaEntityMapper mapper;

    @Override
    public RescueMedia save(
            RescueMedia rescueMedia) {

        return mapper.toDomain(
                jpa.save(
                        mapper.toEntity(rescueMedia)
                )
        );
    }

    @Override
    public List<RescueMedia> findByCaseId(
            UUID caseId) {

        return mapper.toDomainList(
                jpa.findByCaseId(caseId)
        );
    }

    @Override
    public void deleteByCaseId(
            UUID caseId) {

        jpa.deleteByCaseId(caseId);
    }
}
