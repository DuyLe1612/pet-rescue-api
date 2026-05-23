package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.media.MediaFileResponseDto;
import com.uit.petrescueapi.application.port.query.MediaQueryPort;
import com.uit.petrescueapi.domain.entity.MediaFile;
import com.uit.petrescueapi.domain.service.MediaFileDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Query (read) use-case for Media operations.
 *
 * <p>Delegates to {@link MediaFileDomainService} for lookups and maps
 * the domain entity to the response DTO.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaQueryUseCase implements MediaQueryPort {

    private final MediaFileDomainService domainService;
    private final com.uit.petrescueapi.application.port.out.CloudStoragePort cloudStoragePort;

    @Override
    public MediaFileResponseDto findById(UUID mediaId) {
        log.debug("Query: find media file by id {}", mediaId);
        MediaFile media = domainService.findById(mediaId);
        return MediaFileResponseDto.builder()
            .mediaId(media.getMediaId())
            .uploaderId(media.getUploaderId())
            .publicId(media.getPublicId())
            .url(cloudStoragePort.buildUrl(media.getPublicId()))
            .type(media.getResourceType())
            .createdAt(media.getCreatedAt())
            .build();
    }
}
