package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.PetMedia;
import com.uit.petrescueapi.domain.repository.PetMediaRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.PetMediaEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetMediaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PetMediaRepositoryAdapter implements PetMediaRepository {

    private final PetMediaJpaRepository jpa;
    private final PetMediaEntityMapper mapper;

    @Override
    public PetMedia save(PetMedia petMedia) {
        return mapper.toDomain(jpa.save(mapper.toEntity(petMedia)));
    }

    @Override
    public List<PetMedia> findByPetId(UUID petId) {
        return mapper.toDomainList(jpa.findAllByPetId(petId));
    }

    @Override
    public Optional<PetMedia> findPrimaryByPetId(UUID petId) {
        return jpa.findPrimaryByPetId(petId).map(mapper::toDomain);
    }

    @Override
    public Optional<PetMedia> findByPetIdAndMediaFileId(UUID petId, UUID mediaFileId) {
        return jpa.findByPetIdAndMediaFileId(petId, mediaFileId).stream().findFirst().map(mapper::toDomain);
    }

    @Override
    public void clearPrimary(UUID petId) {
        jpa.clearPrimaryByPetId(petId);
    }

    @Override
    public void setPrimary(UUID petId, UUID mediaId) {
        jpa.setPrimaryByPetIdAndMediaId(petId, mediaId);
    }

    @Override
    public void deleteByPetIdAndMediaId(UUID petId, UUID mediaId) {
        jpa.deleteByPetIdAndMediaId(petId, mediaId);
    }

    @Override
    public void delete(UUID mediaId) {
        jpa.deleteById(mediaId);
    }
}
