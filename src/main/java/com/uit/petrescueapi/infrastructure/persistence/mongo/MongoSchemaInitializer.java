package com.uit.petrescueapi.infrastructure.persistence.mongo;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static com.uit.petrescueapi.infrastructure.persistence.mongo.MongoCollections.*;

/**
 * Creates MongoDB validators and indexes on startup — the Mongo counterpart of Flyway.
 * Idempotent: re-creating an identical index is a no-op, validators are re-applied via collMod.
 * Index names are stable so they can be asserted on and dropped deliberately.
 */
@Slf4j
@RequiredArgsConstructor
public class MongoSchemaInitializer implements ApplicationRunner {

    private final MongoTemplate mongo;

    @Override
    public void run(ApplicationArguments args) {
        createValidators();
        createIndexes();
        log.info("MongoDB schema (validators + indexes) is up to date");
    }

    // ── Validators (moderate: only new/updated valid docs are checked) ──

    private void createValidators() {
        validator(CONVERSATIONS, List.of("type", "participantIds", "seq"));
        validator(INBOXES, List.of("userId", "conversationId", "sortAt"));
        validator(MESSAGE_BUCKETS, List.of("conversationId", "bucketNo", "messages"));
        validator(POSTS, List.of("author", "createdAt", "deleted"));
        validator(COMMENTS, List.of("postId", "author", "createdAt", "deleted"));
    }

    private void validator(String collection, List<String> required) {
        Document schema = new Document("$jsonSchema",
                new Document("bsonType", "object").append("required", required));
        MongoDatabase db = mongo.getDb();
        if (!mongo.collectionExists(collection)) {
            db.runCommand(new Document("create", collection)
                    .append("validator", schema)
                    .append("validationLevel", "moderate"));
        } else {
            db.runCommand(new Document("collMod", collection)
                    .append("validator", schema)
                    .append("validationLevel", "moderate"));
        }
    }

    // ── Indexes (migrate.md §2, §3, §5) ──

    private void createIndexes() {
        // chat
        index(CONVERSATIONS, "conversation_direct_key", Indexes.ascending("directKey"),
                new IndexOptions().unique(true).sparse(true));
        index(INBOXES, "inbox_list", Indexes.compoundIndex(
                Indexes.ascending("userId", "hidden"), Indexes.descending("sortAt", "conversationId")));
        index(INBOXES, "inbox_peer", Indexes.ascending("peer.userId"));
        index(MESSAGE_BUCKETS, "bucket_message_id", Indexes.ascending("messages.messageId"));

        // posts
        index(POSTS, "post_feed", Indexes.compoundIndex(
                Indexes.ascending("deleted"), Indexes.descending("createdAt", "_id")));
        index(POSTS, "post_facets", Indexes.compoundIndex(
                        Indexes.ascending("facets.category", "facets.species", "facets.district"),
                        Indexes.descending("createdAt", "_id")),
                new IndexOptions().partialFilterExpression(new Document("deleted", false)));
        index(POSTS, "post_tags", Indexes.compoundIndex(Indexes.ascending("tags"), Indexes.descending("createdAt")));
        index(POSTS, "post_author", Indexes.compoundIndex(Indexes.ascending("author.id"), Indexes.descending("createdAt")));
        index(POSTS, "post_analysis_status", Indexes.ascending("analysis.status", "createdAt"));
        index(POSTS, "post_rescue_case", Indexes.ascending("rescueCase.id"), new IndexOptions().sparse(true));
        index(POSTS, "post_text", Indexes.text("content"), new IndexOptions().defaultLanguage("none"));

        // comments + likes
        index(COMMENTS, "comment_thread", Indexes.compoundIndex(
                Indexes.ascending("postId", "parentId"), Indexes.descending("createdAt", "_id")));
        index(COMMENTS, "comment_author", Indexes.ascending("author.id"));
        index(POST_LIKES, "post_like_user", Indexes.ascending("_id.userId"));
        index(COMMENT_LIKES, "comment_like_user", Indexes.ascending("_id.userId"));

        // DA2 — AI analysis runs (evaluation dataset)
        index(POST_ANALYSIS_RUNS, "run_post", Indexes.compoundIndex(Indexes.ascending("postId"), Indexes.descending("createdAt")));
        index(POST_ANALYSIS_RUNS, "run_prompt_version", Indexes.compoundIndex(Indexes.ascending("promptVersion"), Indexes.descending("createdAt")));
    }

    private void index(String collection, String name, Bson keys) {
        index(collection, name, keys, new IndexOptions());
    }

    private void index(String collection, String name, Bson keys, IndexOptions options) {
        mongo.getCollection(collection).createIndex(keys, options.name(name));
    }
}
