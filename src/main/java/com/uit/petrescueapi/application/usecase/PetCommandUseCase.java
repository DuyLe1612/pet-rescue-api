package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.pet.CreatePetRequestDto;
import com.uit.petrescueapi.application.dto.pet.TransferOwnershipRequestDto;
import com.uit.petrescueapi.application.dto.pet.UpdatePetRequestDto;
import com.uit.petrescueapi.application.port.command.PetCommandPort;
import com.uit.petrescueapi.application.port.command.MediaCommandPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.domain.entity.MediaFile;
import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.PetCurrentOwner;
import com.uit.petrescueapi.domain.entity.PetMedia;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.repository.PetCurrentOwnerRepository;
import com.uit.petrescueapi.domain.repository.PetMediaRepository;
import com.uit.petrescueapi.domain.service.PetDomainService;
import com.uit.petrescueapi.domain.service.OrganizationDomainService;
import com.uit.petrescueapi.domain.service.UserDomainService;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
@Transactional
public class PetCommandUseCase implements PetCommandPort {

    private final PetDomainService petDomainService;
    private final OrganizationDomainService organizationDomainService;
    private final UserDomainService userDomainService;

    private final MediaCommandPort mediaCommandPort;
    private final MediaQueryPort mediaQueryPort;

    private final PetMediaRepository petMediaRepository;
    private final PetCurrentOwnerRepository currentOwnerRepository;

    @Override
    public Pet createForUser(CreatePetRequestDto cmd, UUID userId) {

        log.debug("Command: create pet '{}' for user {}", cmd.getName(), userId);

        UUID petId = UUID.randomUUID();

        Pet pet = buildPetFromDto(cmd, petId);

        Pet created = petDomainService.createForUser(
                pet,
                userId
        );

        attachMedia(
                petId,
                cmd.getMediaIds()
        );

        return created;
    }

    @Override
    public Pet createForShelter(
            CreatePetRequestDto cmd,
            UUID shelterId,
            UUID userId) {

        log.debug(
                "Command: create pet '{}' for shelter {} by user {}",
                cmd.getName(),
                shelterId,
                userId
        );

        if (!organizationDomainService.isMember(
                shelterId,
                userId
        )) {
            throw new ForbiddenException(
                    String.format(
                            "User %s is not a member of organization %s",
                            userId,
                            shelterId
                    )
            );
        }

        UUID petId = UUID.randomUUID();

        Pet pet = buildPetFromDto(cmd, petId);

        Pet created = petDomainService.createForShelter(
                pet,
                shelterId
        );

        attachMedia(
                petId,
                cmd.getMediaIds()
        );

        return created;
    }

    @Override
    public Pet createForUserInOrganization(
            CreatePetRequestDto cmd,
            UUID organizationId,
            UUID userId) {

        log.debug(
                "Command: create pet '{}' in organization {} with user {}",
                cmd.getName(),
                organizationId,
                userId
        );

        if (userId != null) {

            userDomainService.findById(userId);

            if (!organizationDomainService.isMember(
                    organizationId,
                    userId
            )) {
                throw new ForbiddenException(
                        String.format(
                                "User %s is not a member of organization %s",
                                userId,
                                organizationId
                        )
                );
            }
        }

        UUID petId = UUID.randomUUID();

        Pet pet = buildPetFromDto(cmd, petId);
        pet.setShelterId(organizationId);

        Pet created = petDomainService.createForShelter(
                pet,
                organizationId
        );

        attachMedia(
                petId,
                cmd.getMediaIds()
        );

        if (userId != null) {

            currentOwnerRepository.upsert(
                    PetCurrentOwner.builder()
                            .petId(created.getId())
                            .ownerType("ORGANIZATION")
                            .ownerId(organizationId)
                            .caretakerUserId(userId)
                            .build()
            );
        }

        return created;
    }

    private void attachMedia(
            UUID petId,
            List<UUID> mediaIds) {

        if (mediaIds == null || mediaIds.isEmpty()) {
            return;
        }

        for (int i = 0; i < mediaIds.size(); i++) {

            UUID mediaId = mediaIds.get(i);

            var mediaFile = mediaQueryPort.findById(mediaId);

            if (mediaFile == null) {
                throw new BusinessException(
                        "Media not found: " + mediaId
                );
            }

            mediaCommandPort.confirmUpload(
                    mediaId,
                    "pets/" + petId
            );

            PetMedia petMedia = PetMedia.builder()
                    .mediaId(UUID.randomUUID())
                    .petId(petId)
                    .mediaFileId(mediaId)
                    .type(mediaFile.getType())
                    .primaryMedia(i == 0)
                    .createdAt(LocalDateTime.now())
                    .build();

            petMediaRepository.save(petMedia);
        }
    }

    private Pet buildPetFromDto(
            CreatePetRequestDto cmd,
            UUID petId) {

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
                .healthStatus(cmd.getHealthStatus())
                .rescueDate(cmd.getRescueDate())
                .rescueLocation(cmd.getRescueLocation())
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
                .build();

        return petDomainService.update(
                id,
                patch
        );
    }

    @Override
    public void delete(UUID id) {

        log.debug("Command: delete pet {}", id);

        petDomainService.delete(id);
    }

    @Override
    public Pet changeStatus(
            UUID id,
            PetStatus newStatus) {

        log.debug(
                "Command: change status of pet {} to {}",
                id,
                newStatus
        );

        return petDomainService.changeStatus(
                id,
                newStatus
        );
    }

    @Override
    public void transferOwnership(
            UUID petId,
            TransferOwnershipRequestDto cmd,
            UUID requesterId) {

        log.debug(
                "Command: transfer ownership of pet {} to {} {}",
                petId,
                cmd.getNewOwnerType(),
                cmd.getNewOwnerId()
        );

        boolean isAdmin =
                userDomainService.hasRole(
                        requesterId,
                        "ADMIN"
                );

        if (!isAdmin) {

            PetCurrentOwner currentOwner =
                    currentOwnerRepository.findByPetId(petId)
                            .orElseThrow(() ->
                                    new ForbiddenException(
                                            "Pet has no current owner"
                                    ));

            if (!"ORGANIZATION".equals(
                    currentOwner.getOwnerType()
            )) {
                throw new ForbiddenException(
                        "Only admins can transfer user-owned pets"
                );
            }

            if (!organizationDomainService.isOwner(
                    currentOwner.getOwnerId(),
                    requesterId
            )) {
                throw new ForbiddenException(
                        "Only organization owners can transfer pet ownership"
                );
            }
        }

        petDomainService.transferOwnership(
                petId,
                cmd.getNewOwnerType(),
                cmd.getNewOwnerId()
        );
    }
}