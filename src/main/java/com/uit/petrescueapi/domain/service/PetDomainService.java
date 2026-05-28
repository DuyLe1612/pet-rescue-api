package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.PetCurrentOwner;
import com.uit.petrescueapi.domain.entity.PetOwnership;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.PetCurrentOwnerRepository;
import com.uit.petrescueapi.domain.repository.PetOwnershipRepository;
import com.uit.petrescueapi.domain.repository.PetRepository;
import com.uit.petrescueapi.domain.repository.VisualCodeRepository;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Domain service that encapsulates Pet business rules.
 *
 * Key rules implemented here:
 *  • New pets always start with status AVAILABLE.
 *  • Pet creation ATOMICALLY creates ownership record (when shelterId provided).
 *  • Status transitions are validated against an explicit allow-list.
 *  • Deletion is soft-delete (sets `deleted = true`).
 *
 * {@code @Transactional} is placed at the <strong>service layer only</strong>.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PetDomainService {

    private final PetRepository petRepository;
    private final PetOwnershipRepository ownershipRepository;
    private final PetCurrentOwnerRepository currentOwnerRepository;
    private final VisualCodeRepository visualCodeRepository;

    // ── Status-transition matrix ───────────────────
    private static final Map<PetStatus, Set<PetStatus>> ALLOWED_TRANSITIONS = Map.of(
            PetStatus.UNOWNED,   Set.of(PetStatus.PENDING, PetStatus.FOSTERED, PetStatus.UNAVAILABLE, PetStatus.ADOPTED),
            PetStatus.PENDING,     Set.of(PetStatus.ADOPTED, PetStatus.UNOWNED),
            PetStatus.ADOPTED,     Set.of(PetStatus.UNOWNED),
            PetStatus.FOSTERED,    Set.of(PetStatus.UNOWNED, PetStatus.PENDING, PetStatus.ADOPTED),
            PetStatus.UNAVAILABLE, Set.of(PetStatus.UNOWNED, PetStatus.FOSTERED)
    );

    // ── Queries ─────────────────────────────────────

    @Transactional(readOnly = true)
    public Pet findById(UUID id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Pet> findAll() {
        return petRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Pet> findAll(Pageable pageable) {
        return petRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Pet> findAvailable() {
        return petRepository.findByStatus(PetStatus.UNOWNED);
    }

    @Transactional(readOnly = true)
    public Page<Pet> findAvailable(Pageable pageable) {
        return petRepository.findByStatus(PetStatus.UNOWNED, pageable);
    }

    // ── Commands ────────────────────────────────────

    /**
     * Create a pet for a regular user (USER role).
     * Creates Pet + PetOwnership + PetCurrentOwner with ownerType=USER atomically.
     */
    public Pet createForUser(Pet pet, UUID userId) {
        log.info("Creating pet '{}' for user {}", pet.getName(), userId);
        if (pet.getId() == null) {
            pet.setId(UUID.randomUUID());
        }
        pet.setPetCode(visualCodeRepository.nextPetCode());
        pet.setStatus(PetStatus.ADOPTED);
        pet.setCreatedAt(LocalDateTime.now());
        Pet saved = petRepository.save(pet);

        // Atomically create USER ownership
        PetOwnership ownership = PetOwnership.builder()
                .petId(saved.getId())
                .ownerType("USER")
                .ownerId(userId)
                .fromTime(LocalDateTime.now())
                .toTime(null)
                .build();
        ownershipRepository.save(ownership);
        log.debug("Created ownership record: pet {} → user {}", saved.getId(), userId);

        // Update current owner lookup table
        PetCurrentOwner currentOwner = PetCurrentOwner.builder()
                .petId(saved.getId())
                .ownerType("USER")
                .ownerId(userId)
                .caretakerUserId(null)
                .build();
        currentOwnerRepository.upsert(currentOwner);
        log.debug("Updated current owner: pet {} → user {}", saved.getId(), userId);

        return saved;
    }

    /**
     * Create a pet for a shelter/organization (MEMBER role).
     * Creates Pet + PetOwnership + PetCurrentOwner with ownerType=ORGANIZATION atomically.
     */
    public Pet createForShelter(Pet pet, UUID shelterId) {
        log.info("Creating pet '{}' for shelter {}", pet.getName(), shelterId);
        if (pet.getId() == null) {
            pet.setId(UUID.randomUUID());
        }
        pet.setPetCode(visualCodeRepository.nextPetCode());
        pet.setStatus(PetStatus.UNOWNED);
        pet.setCreatedAt(LocalDateTime.now());
        Pet saved = petRepository.save(pet);

        // Atomically create ORGANIZATION ownership
        PetOwnership ownership = PetOwnership.builder()
                .petId(saved.getId())
                .ownerType("ORGANIZATION")
                .ownerId(shelterId)
                .fromTime(LocalDateTime.now())
                .toTime(null)
                .build();
        ownershipRepository.save(ownership);
        log.debug("Created ownership record: pet {} → organization {}", saved.getId(), shelterId);

        // Update current owner lookup table
        PetCurrentOwner currentOwner = PetCurrentOwner.builder()
                .petId(saved.getId())
                .ownerType("ORGANIZATION")
                .ownerId(shelterId)
                .caretakerUserId(null)
                .build();
        currentOwnerRepository.upsert(currentOwner);
        log.debug("Updated current owner: pet {} → organization {}", saved.getId(), shelterId);

        return saved;
    }

    /**
     * Transfer ownership of a pet to a new owner.
     * Ends current ownership and creates new ownership record.
     */
    public void transferOwnership(UUID petId, String newOwnerType, UUID newOwnerId) {
        log.info("Transferring ownership of pet {} to {} {}", petId, newOwnerType, newOwnerId);
        
        // Verify pet exists
        Pet pet = findById(petId);
        LocalDateTime now = LocalDateTime.now();
        
        // End current ownership
        ownershipRepository.endOwnership(petId, now);
        
        // Create new ownership record
        PetOwnership newOwnership = PetOwnership.builder()
                .petId(petId)
                .ownerType(newOwnerType)
                .ownerId(newOwnerId)
                .fromTime(now)
                .toTime(null)
                .build();
        ownershipRepository.save(newOwnership);
        
        // Update current owner lookup table
        PetCurrentOwner currentOwner = PetCurrentOwner.builder()
                .petId(petId)
                .ownerType(newOwnerType)
                .ownerId(newOwnerId)
                .caretakerUserId(null)
                .build();
        currentOwnerRepository.upsert(currentOwner);
        
        log.info("Ownership transferred: pet {} → {} {}", petId, newOwnerType, newOwnerId);
    }

    public Pet update(UUID id, Pet updated) {
        Pet existing = findById(id);
        applyUpdates(existing, updated);
        existing.setUpdatedAt(LocalDateTime.now());
        return petRepository.save(existing);
    }

    public void delete(UUID id) {
        Pet pet = findById(id);
        pet.setDeleted(true);
        pet.setDeletedAt(LocalDateTime.now());
        petRepository.save(pet);
        log.info("Soft-deleted pet {}", id);
    }

    public Pet changeStatus(UUID id, PetStatus newStatus) {
        Pet pet = findById(id);
        PetStatus current = pet.getStatus();

        Set<PetStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition pet status from %s to %s", current, newStatus));
        }

        pet.setStatus(newStatus);
        pet.setUpdatedAt(LocalDateTime.now());
        return petRepository.save(pet);
    }

    // ── Private helpers ─────────────────────────────

    private void applyUpdates(Pet target, Pet source) {
        if (source.getName() != null)        target.setName(source.getName());
        if (source.getSpecies() != null)     target.setSpecies(source.getSpecies());
        if (source.getBreed() != null)       target.setBreed(source.getBreed());
        if (source.getAge() != null)         target.setAge(source.getAge());
        if (source.getGender() != null)      target.setGender(source.getGender());
        if (source.getColor() != null)       target.setColor(source.getColor());
        if (source.getWeight() != null)      target.setWeight(source.getWeight());
        if (source.getDescription() != null) target.setDescription(source.getDescription());
        if (source.getStatus() != null)      target.setStatus(source.getStatus());
        if (source.getHealthStatus() != null)target.setHealthStatus(source.getHealthStatus());
        target.setVaccinated(source.isVaccinated());
        target.setNeutered(source.isNeutered());
        if (source.getImageUrls() != null)   target.setImageUrls(source.getImageUrls());
    }
}
