package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.port.command.MediaCommandPort;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.domain.entity.MediaFile;
import com.uit.petrescueapi.domain.service.MediaFileDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Command (write) use-case for Media operations.
 * Translates parameters into domain calls and delegates business rules
 * to {@link MediaFileDomainService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaCommandUseCase implements MediaCommandPort {

    private final MediaFileDomainService domainService;
    private final com.uit.petrescueapi.domain.service.AccessControlDomainService accessControl;
    private final CloudStoragePort cloudStoragePort;

    @Override
    public MediaFile register(UUID uploaderId, String publicId, String resourceType,
                              String format, Integer width, Integer height,
                              Integer bytes, String folder) {
        log.debug("Command: register media file for uploader {}", uploaderId);
        MediaFile mediaFile = MediaFile.builder()
                .uploaderId(uploaderId)
                .publicId(publicId)
                .resourceType(resourceType)
                .format(format)
                .width(width)
                .height(height)
                .bytes(bytes)
                .folder(folder)
                .status("PERMANENT")
                .build();
        return domainService.register(mediaFile);
    }

    @Override
    public MediaFile uploadTemp(MultipartFile file, UUID uploaderId, String subFolder) {
        log.debug("Command: upload temp media file for uploader {}", uploaderId);
        
        // Upload to Cloudinary temp folder
        CloudStoragePort.CloudUploadResult result = cloudStoragePort.uploadToTemp(file, subFolder);
        
        // Register in database with TEMP status
        MediaFile mediaFile = MediaFile.builder()
                .uploaderId(uploaderId)
                .publicId(result.publicId())
                .resourceType(result.resourceType())
                .format(result.format())
                .width(result.width())
                .height(result.height())
                .bytes(result.bytes())
                .folder(result.folder())
                .status("TEMP")
                .build();
        
        return domainService.register(mediaFile);
    }

    @Override
    public MediaFile confirmUpload(UUID mediaId, String targetFolder) {
        log.debug("Command: confirm media upload {}", mediaId);
        return domainService.confirmUpload(mediaId, targetFolder);
    }

    @Override
    public MediaFile confirmUpload(UUID mediaId, String targetFolder, UUID requesterId) {
        accessControl.requireSelfOrAdmin(domainService.findById(mediaId).getUploaderId(), requesterId);
        return confirmUpload(mediaId, targetFolder);
    }

    @Override
    public void delete(UUID mediaId, UUID requesterId) {
        accessControl.requireSelfOrAdmin(domainService.findById(mediaId).getUploaderId(), requesterId);
        delete(mediaId);
    }

    @Override
    public void delete(UUID mediaId) {
        log.debug("Command: delete media file {}", mediaId);
        domainService.delete(mediaId);
    }
}
