package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.domain.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Cloudinary adapter implementing the CloudStoragePort.
 * Lives in persistence/adapter since it's an infrastructure adapter.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageAdapter implements CloudStoragePort {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.temp-folder:temp}")
    private String tempFolder;

    @Value("${cloudinary.permanent-folder:pet-rescue}")
    private String permanentFolder;

    @Override
    public CloudUploadResult uploadToTemp(MultipartFile file, String subFolder) {
        try {
            String folder = tempFolder + (subFolder != null ? "/" + subFolder : "");
            log.info("Uploading file to temp folder: {}", folder);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"
                    ));

            String publicId = (String) result.get("public_id");
            String url = (String) result.get("secure_url");
            String resourceType = (String) result.get("resource_type");
            String format = (String) result.get("format");
            Integer width = result.get("width") != null ? ((Number) result.get("width")).intValue() : null;
            Integer height = result.get("height") != null ? ((Number) result.get("height")).intValue() : null;
            Integer bytes = result.get("bytes") != null ? ((Number) result.get("bytes")).intValue() : null;

            log.info("Successfully uploaded file with publicId: {}", publicId);

            return new CloudUploadResult(
                    publicId,
                    url,
                    resourceType,
                    format,
                    width,
                    height,
                    bytes,
                    folder
            );
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new BusinessException("Failed to upload file: " + e.getMessage());
        }
    }

    @Override
    public String moveToPermament(String publicId, String targetSubFolder) {
        try {
            String targetFolder = permanentFolder + (targetSubFolder != null ? "/" + targetSubFolder : "");
            String newPublicId = targetFolder + "/" + extractFileName(publicId);

            log.info("Moving file from {} to {}", publicId, newPublicId);

            cloudinary.uploader().rename(publicId, newPublicId, ObjectUtils.emptyMap());

            log.info("Successfully moved file to: {}", newPublicId);
            return newPublicId;
        } catch (IOException e) {
            log.error("Failed to move file in Cloudinary", e);
            throw new BusinessException("Failed to move file: " + e.getMessage());
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            log.info("Deleting file with publicId: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Successfully deleted file: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary", e);
            throw new BusinessException("Failed to delete file: " + e.getMessage());
        }
    }

    @Override
    public String buildUrl(String publicId) {
        return cloudinary.url().generate(publicId);
    }

    @Override
    public String buildUploadUrl() {
        return "https://api.cloudinary.com/v1_1/" + cloudinary.config.cloudName + "/auto/upload";
    }

    @Override
    public SignedUploadResult createSignedUpload(String folder, String publicId) {
        long timestamp = System.currentTimeMillis() / 1000;
        Map<String, Object> params = ObjectUtils.asMap(
                "timestamp", timestamp,
                "folder", folder,
                "public_id", publicId
        );
        String signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret);
        return new SignedUploadResult(
                cloudinary.config.apiKey,
                signature,
                timestamp,
                folder,
                publicId
        );
    }

    private String extractFileName(String publicId) {
        int lastSlash = publicId.lastIndexOf('/');
        return lastSlash >= 0 ? publicId.substring(lastSlash + 1) : publicId;
    }
}
