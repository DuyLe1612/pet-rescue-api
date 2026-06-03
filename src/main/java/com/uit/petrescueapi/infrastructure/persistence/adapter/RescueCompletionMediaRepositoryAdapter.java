package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.RescueCaseCompletion;
import com.uit.petrescueapi.domain.entity.RescueCompletionMedia;
import com.uit.petrescueapi.domain.repository.RescueCaseCompletionRepository;
import com.uit.petrescueapi.domain.repository.RescueCompletionMediaRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.RescueCaseCompletionEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.mapper.RescueCompletionMediaEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.RescueCaseCompletionJpaRepository;
import com.uit.petrescueapi.infrastructure.persistence.repository.RescueCompletionMediaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RescueCompletionMediaRepositoryAdapter
        implements RescueCompletionMediaRepository {

    private final RescueCompletionMediaJpaRepository jpa;
    private final RescueCompletionMediaEntityMapper mapper;

    @Override
    public RescueCompletionMedia save(
            RescueCompletionMedia media) {

        return mapper.toDomain(
                jpa.save(
                        mapper.toEntity(media)
                )
        );
    }

    @Override
    public List<RescueCompletionMedia> findByCompletionId(
            UUID completionId) {

        return mapper.toDomainList(
                jpa.findByCompletionId(completionId)
        );
    }

    @Override
    public void deleteByCompletionId(
            UUID completionId) {

        jpa.deleteByCompletionId(completionId);
    }
}
