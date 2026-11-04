package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.pet.CreateMedicalRecordRequestDto;
import com.uit.petrescueapi.application.port.command.PetDetailsCommandPort;
import com.uit.petrescueapi.application.port.command.MediaCommandPort;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.domain.entity.Pet;
import com.uit.petrescueapi.domain.entity.PetMedicalRecord;
import com.uit.petrescueapi.domain.entity.PetMedia;
import com.uit.petrescueapi.domain.exception.ForbiddenException;
import com.uit.petrescueapi.domain.repository.PetMedicalRecordRepository;
import com.uit.petrescueapi.domain.repository.PetMediaRepository;
import com.uit.petrescueapi.domain.service.AccessControlDomainService;
import com.uit.petrescueapi.domain.service.PetDomainService;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Command (write) use-case for Pet sub-resource operations.
 * Handles medical records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PetDetailsCommandUseCase implements PetDetailsCommandPort {

    private final PetMedicalRecordRepository medicalRecordRepository;
    private final PetMediaRepository petMediaRepository;
    private final MediaCommandPort mediaCommandPort;
    private final MediaQueryPort mediaQueryPort;
    private final PetDomainService petDomainService;
    private final AccessControlDomainService accessControl;

    @Override
    public PetMedicalRecord addMedicalRecord(UUID petId, CreateMedicalRecordRequestDto cmd, UUID requesterId) {
        log.debug("Command: add medical record for pet {}", petId);
        ensureCanManagePet(petId, requesterId);
        PetMedicalRecord record = PetMedicalRecord.builder()
                .recordId(UUID.randomUUID())
                .petId(petId)
                .recordType(cmd.getRecordType())
                .description(cmd.getDescription())
                .vaccine(cmd.getVaccine())
                .diagnosis(cmd.getDiagnosis())
                .recordDate(cmd.getRecordDate() != null
                        ? LocalDate.parse(cmd.getRecordDate()).atStartOfDay()
                        : null)
                .createdAt(LocalDateTime.now())
                .build();
        return medicalRecordRepository.save(record);
    }

    /** Uploading can take seconds, so this runs outside a transaction; only the final link needs one. */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PetMedia addMedia(UUID petId, MultipartFile file, boolean primary, UUID requesterId) {
        ensureCanManagePet(petId, requesterId);
        var mediaFile = mediaCommandPort.uploadTemp(file, requesterId, "pets/" + petId);
        mediaCommandPort.confirmUpload(mediaFile.getMediaId(), "pets/" + petId);
        return petDomainService.linkMedia(petId, mediaFile.getMediaId(), mediaFile.getResourceType(), primary);
    }
@Override
    public PetMedia attachMedia(UUID petId, UUID mediaFileId, boolean primary, UUID requesterId) {
        ensureCanManagePet(petId, requesterId);
        var media = mediaQueryPort.findById(mediaFileId);
        petDomainService.findById(petId);

        if (primary) {
            petMediaRepository.clearPrimary(petId);
        }

        PetMedia petMedia = PetMedia.builder()
                .mediaId(UUID.randomUUID())
                .petId(petId)
                .mediaFileId(mediaFileId)
            .type(media.getType())
                .primaryMedia(primary)
                .createdAt(LocalDateTime.now())
                .build();
        return petMediaRepository.save(petMedia);
    }

    @Override
    public PetMedia setPrimaryMedia(UUID petId, UUID mediaId, UUID requesterId) {
        ensureCanManagePet(petId, requesterId);
        petMediaRepository.findByPetIdAndMediaFileId(petId, mediaId)
                .orElseThrow(() -> new ForbiddenException("Media does not belong to pet"));
        petMediaRepository.clearPrimary(petId);
        petMediaRepository.setPrimary(petId, mediaId);
        return petMediaRepository.findByPetIdAndMediaFileId(petId, mediaId).orElseThrow();
    }

    @Override
    public void deleteMedia(UUID petId, UUID mediaId, UUID requesterId) {
        ensureCanManagePet(petId, requesterId);
        petMediaRepository.findByPetIdAndMediaFileId(petId, mediaId)
                .orElseThrow(() -> new ForbiddenException("Media does not belong to pet"));
        petMediaRepository.deleteByPetIdAndMediaId(petId, mediaId);
        mediaCommandPort.delete(mediaId);
    }

    private void ensureCanManagePet(UUID petId, UUID requesterId) {
        petDomainService.findById(petId);
        accessControl.requirePetManager(petId, requesterId);
    }
}
