package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.pet.CreatePetRequestDto;
import com.uit.petrescueapi.application.dto.pet.TransferOwnershipRequestDto;
import com.uit.petrescueapi.application.dto.pet.UpdatePetRequestDto;
import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.valueobject.PetStatus;

import java.util.UUID;

/**
 * Command (write) port for Pet operations.
 * Handles all state-changing operations on the Pet aggregate.
 */
public interface PetCommandPort {

    /**
     * Create pet for a regular user (USER role).
     * @param cmd Pet data
     * @param userId ID of the user creating the pet
     * @return Created pet with USER ownership
     */
    Pet createForUser(CreatePetRequestDto cmd, UUID userId);

    /**
     * Create pet for a shelter/organization (MEMBER role).
     * @param cmd Pet data
     * @param shelterId ID of the organization creating the pet
     * @param userId ID of the user creating the pet (for membership validation)
     * @return Created pet with ORGANIZATION ownership
     */
    Pet createForShelter(CreatePetRequestDto cmd, UUID shelterId, UUID userId);

    /**
     * Create a pet under an organization context but assign current ownership to a user.
     * Intended for administrative operations.
     */
    Pet createForUserInOrganization(CreatePetRequestDto cmd, UUID organizationId, UUID userId);

    Pet update(UUID id, UpdatePetRequestDto cmd, UUID requesterId);

    void delete(UUID id, UUID requesterId);

    Pet changeStatus(UUID id, PetStatus newStatus, UUID requesterId);

    /**
     * Manually transfer pet ownership to a new owner.
     * Only admins and organization owners can perform this operation.
     *
     * @param petId ID of the pet to transfer
     * @param cmd Transfer ownership request containing new owner details
     * @param requesterId ID of the user making the request (for authorization)
     */
    void transferOwnership(UUID petId, TransferOwnershipRequestDto cmd, UUID requesterId);
}
