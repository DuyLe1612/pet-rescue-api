package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;
import com.uit.petrescueapi.domain.repository.RescueCaseCompletionRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.RescueCaseCompletionEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.RescueCaseCompletionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RescueCaseCompletionRepositoryAdapter implements RescueCaseCompletionRepository {

    private final RescueCaseCompletionJpaRepository jpa;
    private final RescueCaseCompletionEntityMapper mapper;

    @Override
    public RescueCaseCompletion Save(RescueCaseCompletion rescueCaseCompletion) {
        var entity = mapper.toEntity(rescueCaseCompletion);
        var savedEntity = jpa.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<RescueCaseCompletion> findById(UUID id) {
        return jpa.findById(id)
                .filter(e -> !e.isDeleted())
                .map(mapper::toDomain);
    }

    @Override
    public Page<RescueCaseCompletion> findAll(Pageable pageable) {
        return jpa.findByDeletedFalse(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RescueCaseCompletion> findByCaseId(UUID caseId) {
        return jpa.findByCaseIdAndDeletedFalse(caseId)
                .map(mapper::toDomain);
    }
}
