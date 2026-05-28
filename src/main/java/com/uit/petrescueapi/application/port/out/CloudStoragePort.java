package com.uit.petrescueapi.application.port.out;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Output port for cloud storage operations (Cloudinary).
 * This is an infrastructure concern abstracted for the application layer.
 */
public interface CloudStoragePort {

    /**
     * Upload a file to temporary storage.
     *
     * @param file   The file to upload
     * @param folder The folder path in cloud storage
     * @return Upload result containing publicId, url, and metadata
     */
    CloudUploadResult uploadToTemp(MultipartFile file, String folder);

    /**
     * Move a file from temp to permanent storage.
     *
     * @param publicId Current public ID of the file
     * @param targetFolder Target folder for permanent storage
     * @return New public ID after move
     */
    String moveToPermament(String publicId, String targetFolder, String resourceType);

    /**
     * Delete a file from cloud storage.
     *
     * @param publicId The public ID of the file to delete
     */
    void delete(String publicId);

    /**
     * Build a URL from a public ID.
     *
     * @param publicId The public ID
     * @return Full URL to the resource
     */
    String buildUrl(String publicId);

    /**
     * Build an upload URL for signed direct uploads.
     */
    String buildUploadUrl();

    /**
     * Create a signed upload payload for direct client uploads.
     */
    SignedUploadResult createSignedUpload(String folder, String publicId);

    /**
     * Result of a cloud upload operation.
     */
    record CloudUploadResult(
            String publicId,
            String url,
            String resourceType,
            String format,
            Integer width,
            Integer height,
            Integer bytes,
            String folder
    ) {}

        record SignedUploadResult(
            String apiKey,
            String signature,
            long timestamp,
            String folder,
            String publicId
        ) {}
}
