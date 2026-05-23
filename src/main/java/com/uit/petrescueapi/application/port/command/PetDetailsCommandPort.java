package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.pet.CreateMedicalRecordRequestDto;
import com.uit.petrescueapi.domain.entity.PetMedicalRecord;
import com.uit.petrescueapi.domain.entity.PetMedia;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface PetDetailsCommandPort {
    PetMedicalRecord addMedicalRecord(UUID petId, CreateMedicalRecordRequestDto cmd);

    PetMedia addMedia(UUID petId, MultipartFile file, boolean primary, UUID requesterId);

    PetMedia attachMedia(UUID petId, UUID mediaFileId, boolean primary, UUID requesterId);

    PetMedia setPrimaryMedia(UUID petId, UUID mediaId, UUID requesterId);

    void deleteMedia(UUID petId, UUID mediaId, UUID requesterId);
}
