package com.uit.petrescueapi.infrastructure.persistence.mongo;

import com.uit.petrescueapi.support.MongoIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MongoSchemaInitializerTest extends MongoIntegrationTest {

    @Autowired
    MongoTemplate mongo;

    @BeforeEach
    void init() {
        mongo.getDb().drop();
        new MongoSchemaInitializer(mongo).run(null);
    }

    private List<String> indexNames(String collection) {
        return mongo.getCollection(collection).listIndexes().map(d -> d.getString("name")).into(new java.util.ArrayList<>());
    }

    @Test
    void createsReadPathIndexes() {
        assertThat(indexNames(MongoCollections.INBOXES)).contains("inbox_list", "inbox_conversation"); // every send updates by conversationId
        assertThat(indexNames(MongoCollections.COMMENTS)).contains("comment_replies");                 // replies are queried by parentId alone
        assertThat(indexNames(MongoCollections.CONVERSATIONS)).contains("conversation_direct_key");
        assertThat(indexNames(MongoCollections.MESSAGE_BUCKETS)).contains("bucket_message_id");
        assertThat(indexNames(MongoCollections.POSTS)).contains("post_feed", "post_facets", "post_tags",
                "post_author", "post_analysis_status", "post_rescue_case", "post_text");
        assertThat(indexNames(MongoCollections.COMMENTS)).contains("comment_thread", "comment_author");
        assertThat(indexNames(MongoCollections.POST_LIKES)).contains("post_like_user");
        assertThat(indexNames(MongoCollections.COMMENT_LIKES)).contains("comment_like_user");
        assertThat(indexNames(MongoCollections.POST_ANALYSIS_RUNS)).contains("run_post", "run_prompt_version");
    }

    private static Document conversation(String directKey) {
        return new Document("_id", UUID.randomUUID()).append("type", "GENERAL")
                .append("participantIds", List.of()).append("seq", 0L).append("directKey", directKey);
    }

    @Test
    void directKeyIsUnique() {
        mongo.getCollection(MongoCollections.CONVERSATIONS).insertOne(conversation("GENERAL:-:a:b"));
        assertThatThrownBy(() -> mongo.getCollection(MongoCollections.CONVERSATIONS).insertOne(conversation("GENERAL:-:a:b")))
                .hasMessageContaining("duplicate key");
    }

    @Test
    void validatorRejectsInboxWithoutUserId() {
        assertThatThrownBy(() -> mongo.getCollection(MongoCollections.INBOXES)
                .insertOne(new Document("_id", "x:y").append("conversationId", UUID.randomUUID())))
                .hasMessageContaining("Document failed validation");
    }

    @Test
    void isIdempotent() {
        new MongoSchemaInitializer(mongo).run(null);
        assertThat(indexNames(MongoCollections.POSTS)).contains("post_feed");
    }
}
