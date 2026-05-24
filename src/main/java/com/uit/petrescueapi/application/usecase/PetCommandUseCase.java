package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.pet.CreatePetRequestDto;
import com.uit.petrescueapi.application.dto.pet.TransferOwnershipRequestDto;
import com.uit.petrescueapi.application.dto.pet.UpdatePetRequestDto;
import com.uit.petrescueapi.application.port.command.PetCommandPort;
import com.uit.petrescueapi.application.port.command.MediaCommandPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.PetCurrentOwner;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.repository.PetCurrentOwnerRepository;
import com.uit.petrescueapi.domain.service.PetDomainService;
import com.uit.petrescueapi.domain.service.OrganizationDomainService;
import com.uit.petrescueapi.domain.service.UserDomainService;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

/**
 * Command (write) use-case for Pet operations.
 * Translates request DTOs into domain calls and delegates business rules
 * to {@link PetDomainService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PetCommandUseCase implements PetCommandPort {

    private final PetDomainService domainService;
    private final OrganizationDomainService organizationDomainService;
    private final UserDomainService userDomainService;
    private final MediaCommandPort mediaCommandPort;
    private final MediaQueryPort mediaQueryPort;
    private final PetCurrentOwnerRepository currentOwnerRepository;

    @Override
    public Pet createForUser(CreatePetRequestDto cmd, UUID userId) {
        log.debug("Command: create pet '{}' for user {}", cmd.getName(), userId);
        UUID petId = UUID.randomUUID();
        Pet pet = buildPetFromDto(cmd, petId);
        return domainService.createForUser(pet, userId);
    }

    @Override
    public Pet createForShelter(CreatePetRequestDto cmd, UUID shelterId, UUID userId) {
        log.debug("Command: create pet '{}' for shelter {} by user {}", cmd.getName(), shelterId, userId);

        // Validate membership in application layer
        if (!organizationDomainService.isMember(shelterId, userId)) {
            throw new ForbiddenException(
                    String.format("User %s is not a member of organization %s", userId, shelterId));
        }

        UUID petId = UUID.randomUUID();
        Pet pet = buildPetFromDto(cmd, petId);
        return domainService.createForShelter(pet, shelterId);
    }

    @Override
    public Pet createForUserInOrganization(CreatePetRequestDto cmd, UUID organizationId, UUID userId) {
        log.debug("Command: create pet '{}' in organization {} with user {}", cmd.getName(), organizationId, userId);

        if (userId != null) {
            // Ensure target user exists.
            userDomainService.findById(userId);

            // Ensure user belongs to the organization context.
            if (!organizationDomainService.isMember(organizationId, userId)) {
                throw new ForbiddenException(
                        String.format("User %s is not a member of organization %s", userId, organizationId));
            }
        }

        UUID petId = UUID.randomUUID();
        Pet pet = buildPetFromDto(cmd, petId);
        pet.setShelterId(organizationId);
        Pet created = domainService.createForShelter(pet, organizationId);

        if (userId != null) {
            currentOwnerRepository.upsert(PetCurrentOwner.builder()
                .petId(created.getId())
                .ownerType("ORGANIZATION")
                .ownerId(organizationId)
                .caretakerUserId(userId)
                .build());
        }

        return created;
    }

    private Pet buildPetFromDto(CreatePetRequestDto cmd, UUID petId) {
        return Pet.builder()
                .id(petId)
                .name(cmd.getName())
                .species(cmd.getSpecies())
                .breed(cmd.getBreed())
                .age(cmd.getAge())
                .gender(cmd.getGender())
                .color(cmd.getColor())
                .weight(cmd.getWeight())
                .description(cmd.getDescription())
                .vaccinated(cmd.isVaccinated())
                .neutered(cmd.isNeutered())
                .rescueDate(cmd.getRescueDate())
                .rescueLocation(cmd.getRescueLocation())
                .imageUrls(resolveImageUrls(cmd.getMediaIds(), cmd.getImageUrls(), petId))
                .rescueCaseId(cmd.getRescueCaseId())
                .build();
    }

    @Override
    public Pet update(UUID id, UpdatePetRequestDto cmd) {
        log.debug("Command: update pet {}", id);
        Pet patch = Pet.builder()
            .id(id)
                .name(cmd.getName())
                .species(cmd.getSpecies())
                .breed(cmd.getBreed())
                .age(cmd.getAge())
                .gender(cmd.getGender())
                .color(cmd.getColor())
                .weight(cmd.getWeight())
                .description(cmd.getDescription())
                .status(cmd.getStatus())
                .healthStatus(cmd.getHealthStatus())
                .vaccinated(cmd.isVaccinated())
                .neutered(cmd.isNeutered())
                .imageUrls(resolveImageUrls(cmd.getMediaIds(), cmd.getImageUrls(), id))
                .build();
        return domainService.update(id, patch);
    }

    private java.util.List<String> resolveImageUrls(java.util.List<UUID> mediaIds, java.util.List<String> imageUrls, UUID petId) {
        if (mediaIds != null && !mediaIds.isEmpty()) {
            List<String> resolved = new ArrayList<>();
            for (UUID mediaId : mediaIds) {
                try {
                    mediaCommandPort.confirmUpload(mediaId, "pets/" + petId);
                } catch (RuntimeException ignored) {
                    // Already permanent or otherwise not confirmable; keep using the current record.
                }
                resolved.add(mediaQueryPort.findById(mediaId).getUrl());
            }
            return resolved;
        }
        return imageUrls;
    }

    @Override
    public void delete(UUID id) {
        log.debug("Command: delete pet {}", id);
        domainService.delete(id);
    }

    @Override
    public Pet changeStatus(UUID id, PetStatus newStatus) {
        log.debug("Command: change status of pet {} to {}", id, newStatus);
        return domainService.changeStatus(id, newStatus);
    }

    @Override
    public void transferOwnership(UUID petId, TransferOwnershipRequestDto cmd, UUID requesterId) {
        log.debug("Command: transfer ownership of pet {} to {} {}", petId, cmd.getNewOwnerType(), cmd.getNewOwnerId());

        // Authorization check: must be admin or owner of the pet's organization
        boolean isAdmin = userDomainService.hasRole(requesterId, "ADMIN");

        if (!isAdmin) {
            // Check if requester is owner of the organization that currently owns the pet
            PetCurrentOwner currentOwner = currentOwnerRepository.findByPetId(petId)
                    .orElseThrow(() -> new ForbiddenException("Pet has no current owner"));

            if (!"ORGANIZATION".equals(currentOwner.getOwnerType())) {
                throw new ForbiddenException("Only admins can transfer user-owned pets");
            }

            if (!organizationDomainService.isOwner(currentOwner.getOwnerId(), requesterId)) {
                throw new ForbiddenException("Only organization owners can transfer pet ownership");
            }
        }

        domainService.transferOwnership(petId, cmd.getNewOwnerType(), cmd.getNewOwnerId());
    }
}
