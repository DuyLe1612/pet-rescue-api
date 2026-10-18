package com.uit.petrescueapi.application.port.out;

import java.util.List;
import java.util.UUID;

/**
 * Attaches uploaded media to a post: TEMP uploads are promoted to permanent storage in the
 * post's folder (their public id changes), permanent media are used as they are. Idempotent,
 * so a retried MongoDB transaction can call it again safely.
 */
public interface PostMediaPort {

    /** In input order; throws {@code BusinessException} if any media id is unknown, before moving anything. */
    List<AttachedMedia> attach(UUID postId, List<UUID> mediaIds);

    record AttachedMedia(UUID mediaId, String publicId) {
    }
}
