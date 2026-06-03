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
public class RescueCaseCompletionRepositoryAdapter
        implements RescueCaseCompletionRepository {

    private final RescueCaseCompletionJpaRepository jpa;
    private final RescueCaseCompletionEntityMapper mapper;

    @Override
    public RescueCaseCompletion save(
            RescueCaseCompletion completion) {

        return mapper.toDomain(
                jpa.save(
                        mapper.toEntity(completion)
                )
        );
    }

    @Override
    public Optional<RescueCaseCompletion> findById(UUID id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Page<RescueCaseCompletion> findAll(Pageable pageable) {
        return jpa.findAll(pageable)
                .map(mapper::toDomain);
    }
    @Override
    public Optional<RescueCaseCompletion> findByCaseId(
            UUID caseId) {

        return jpa.findByCaseIdAndDeletedFalse(caseId)
                .map(mapper::toDomain);
    }
}
