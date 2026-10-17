package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.repository.PetRepository;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import com.uit.petrescueapi.infrastructure.persistence.entity.PetJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.mapper.PetEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter that wires the domain repository port ({@link PetRepository})
 * to the real JPA repository.
 *
 * ⚠️ No {@code @Transactional} here — transactions are managed at the service layer.
 */
@Component
@RequiredArgsConstructor
public class PetRepositoryAdapter implements PetRepository {

    private final PetJpaRepository jpa;
    private final PetEntityMapper mapper;

    @Override
    public Pet save(Pet pet) {
        PetJpaEntity entity = mapper.toEntity(pet);
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Pet> findById(UUID id) {
        return jpa.findByIdAndNotDeleted(id).map(mapper::toDomain);
    }

    @Override
    public List<Pet> findAll() {
        return mapper.toDomainList(jpa.findAllNotDeleted());
    }

    @Override
    public Page<Pet> findAll(Pageable pageable) {
        return jpa.findAllNotDeleted(pageable).map(mapper::toDomain);
    }

    @Override
    public List<Pet> findByStatus(PetStatus status) {
        return mapper.toDomainList(jpa.findByStatus(status));
    }

    @Override
    public Page<Pet> findByStatus(PetStatus status, Pageable pageable) {
        return jpa.findByStatus(status, pageable).map(mapper::toDomain);
    }
}
