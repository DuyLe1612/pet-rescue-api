package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.PetOwnership;
import com.uit.petrescueapi.domain.repository.PetOwnershipRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.PetOwnershipEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetOwnershipJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PetOwnershipRepositoryAdapter implements PetOwnershipRepository {

    private final PetOwnershipJpaRepository jpa;
    private final PetOwnershipEntityMapper mapper;

    @Override
    public PetOwnership save(PetOwnership ownership) {
        return mapper.toDomain(jpa.save(mapper.toEntity(ownership)));
    }

    @Override
    @Transactional
    public void endOwnership(UUID petId, LocalDateTime endTime) {
        jpa.endCurrentOwnership(petId, endTime);
    }
}
