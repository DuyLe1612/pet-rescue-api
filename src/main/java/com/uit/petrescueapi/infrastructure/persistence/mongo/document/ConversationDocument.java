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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Small write-coordination document: issues per-conversation message seqs and holds
 * every participant's read watermark (migrate.md §2.1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(MongoCollections.CONVERSATIONS)
public class ConversationDocument {
    @Id
    private UUID id;
    private String type;
    private String name;
    private UUID relatedEntityId;
    private String relatedInfo;
    @Builder.Default
    private List<UUID> participantIds = new ArrayList<>();
    private String directKey;
    /** Last issued message seq (contiguous 1..n). */
    private long seq;
    /** userId (string) to highest seq that user has read. */
    @Builder.Default
    private Map<String, Long> readSeq = new HashMap<>();
    private LastMessageDocument lastMessage;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private UUID createdBy;
    private LocalDateTime updatedAt;
    private UUID updatedBy;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private UUID deletedBy;

    public long readSeqOf(UUID userId) {
        Long value = readSeq == null ? null : readSeq.get(userId.toString());
        return value == null ? 0L : value;
    }

    /** Type + related entity + sorted pair: identical for both directions of a direct chat. */
    public static String directKeyOf(String type, UUID relatedEntityId, UUID a, UUID b) {
        String first = a.toString().compareTo(b.toString()) <= 0 ? a.toString() : b.toString();
        String second = first.equals(a.toString()) ? b.toString() : a.toString();
        return type + ":" + (relatedEntityId == null ? "-" : relatedEntityId) + ":" + first + ":" + second;
    }
}
