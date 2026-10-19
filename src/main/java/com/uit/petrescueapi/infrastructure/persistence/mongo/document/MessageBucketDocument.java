package com.uit.petrescueapi.infrastructure.persistence.mongo.document;

import com.uit.petrescueapi.infrastructure.persistence.mongo.MongoCollections;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Up to {@link #SIZE} consecutive messages of one conversation (bucket pattern, migrate.md §2.3).
 * {@code _id = "<conversationId>:<bucketNo>"}, {@code bucketNo = (seq - 1) / SIZE}, so the bucket
 * holding any seq is computable without a lookup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(MongoCollections.MESSAGE_BUCKETS)
public class MessageBucketDocument {

    public static final int SIZE = 50;

    @Id
    private String id;
    private UUID conversationId;
    private long bucketNo;
    @Builder.Default
    private List<MessageDocument> messages = new ArrayList<>();

    public static long bucketNo(long seq) {
        return (seq - 1) / SIZE;
    }

    public static String idOf(UUID conversationId, long bucketNo) {
        return conversationId + ":" + bucketNo;
    }

    public static String idForSeq(UUID conversationId, long seq) {
        return idOf(conversationId, bucketNo(seq));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageDocument {
        /** Not named {@code id}: Spring Data would map that to {@code _id}. */
        private UUID messageId;
        private long seq;
        private UUID senderId;
        private String content;
        private LocalDateTime sentAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean deleted;
        private LocalDateTime deletedAt;
        private UUID deletedBy;
    }
}
