package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.infrastructure.persistence.mongo.document.MessageBucketDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.MessageBucketDocument.MessageDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Reads seq ranges from message buckets. Each round fetches two adjacent buckets by {@code _id};
 * deleted messages are skipped and further buckets are read until the page is full.
 */
@Component
@RequiredArgsConstructor
public class MessageBucketReader {

    private final MongoTemplate mongo;

    /** Non-deleted messages with {@code seq < beforeSeq}, newest first. */
    public List<MessageDocument> before(UUID conversationId, long beforeSeq, int size) {
        List<MessageDocument> result = new ArrayList<>();
        long hi = beforeSeq - 1;
        if (hi < 1 || size <= 0) {
            return result;
        }
        long bucket = MessageBucketDocument.bucketNo(hi);
        while (result.size() < size && bucket >= 0) {
            List<MessageDocument> chunk = load(conversationId, List.of(bucket, bucket - 1));
            chunk.sort(Comparator.comparingLong(MessageDocument::getSeq).reversed());
            for (MessageDocument m : chunk) {
                if (m.getSeq() <= hi && !m.isDeleted() && result.size() < size) {
                    result.add(m);
                }
            }
            bucket -= 2;
        }
        return result;
    }

    /** Non-deleted messages with {@code afterSeq < seq <= maxSeq}, oldest first. */
    public List<MessageDocument> after(UUID conversationId, long afterSeq, long maxSeq, int size) {
        List<MessageDocument> result = new ArrayList<>();
        if (afterSeq >= maxSeq || size <= 0) {
            return result;
        }
        long bucket = MessageBucketDocument.bucketNo(afterSeq + 1);
        long lastBucket = MessageBucketDocument.bucketNo(maxSeq);
        while (result.size() < size && bucket <= lastBucket) {
            List<MessageDocument> chunk = load(conversationId, List.of(bucket, bucket + 1));
            chunk.sort(Comparator.comparingLong(MessageDocument::getSeq));
            for (MessageDocument m : chunk) {
                if (m.getSeq() > afterSeq && !m.isDeleted() && result.size() < size) {
                    result.add(m);
                }
            }
            bucket += 2;
        }
        return result;
    }

    private List<MessageDocument> load(UUID conversationId, List<Long> bucketNos) {
        List<String> ids = bucketNos.stream()
                .filter(no -> no >= 0)
                .map(no -> MessageBucketDocument.idOf(conversationId, no))
                .toList();
        List<MessageDocument> messages = new ArrayList<>();
        mongo.find(query(where("_id").in(ids)), MessageBucketDocument.class)
                .forEach(b -> messages.addAll(b.getMessages()));
        return messages;
    }
}
