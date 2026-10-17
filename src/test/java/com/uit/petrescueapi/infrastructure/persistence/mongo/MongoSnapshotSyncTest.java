package com.uit.petrescueapi.infrastructure.persistence.mongo;

import com.uit.petrescueapi.application.port.out.UserSnapshotPort;
import com.uit.petrescueapi.application.port.out.UserSnapshotPort.UserSnapshot;
import com.uit.petrescueapi.domain.event.RescueCaseStatusChangedEvent;
import com.uit.petrescueapi.domain.event.UserProfileChangedEvent;
import com.uit.petrescueapi.support.MongoIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MongoSnapshotSyncTest extends MongoIntegrationTest {

    static final UUID ALICE = UUID.randomUUID();
    static final UUID BOB = UUID.randomUUID();
    static final UUID CASE = UUID.randomUUID();

    @Autowired MongoTemplate mongo;

    final Map<UUID, UserSnapshot> users = new HashMap<>();
    MongoSnapshotSyncListener listener;

    @BeforeEach
    void seed() {
        mongo.getDb().drop();
        UserSnapshotPort port = ids -> {
            Map<UUID, UserSnapshot> r = new HashMap<>();
            ids.forEach(id -> { if (users.containsKey(id)) r.put(id, users.get(id)); });
            return r;
        };
        MockEnvironment env = new MockEnvironment().withProperty("app.storage.chat", "mongo").withProperty("app.storage.post", "mongo");
        listener = new MongoSnapshotSyncListener(mongo, port, env);

        Document alice = author(ALICE, "alice");
        mongo.getCollection(MongoCollections.POSTS).insertMany(List.of(
                new Document("_id", UUID.randomUUID()).append("author", alice)
                        .append("previewComments", List.of(
                                new Document("commentId", UUID.randomUUID()).append("author", author(BOB, "bob")),
                                new Document("commentId", UUID.randomUUID()).append("author", author(ALICE, "alice"))))
                        .append("rescueCase", new Document("caseId", CASE).append("status", "REPORTED")),
                new Document("_id", UUID.randomUUID()).append("author", author(BOB, "bob")).append("previewComments", List.of())));
        mongo.getCollection(MongoCollections.COMMENTS).insertOne(new Document("_id", UUID.randomUUID()).append("author", author(ALICE, "alice")));
        mongo.getCollection(MongoCollections.INBOXES).insertMany(List.of(
                new Document("_id", "b:c").append("userId", BOB).append("peer", new Document("userId", ALICE).append("name", "alice")),
                new Document("_id", "a:c").append("userId", ALICE).append("peer", new Document("userId", BOB).append("name", "bob"))));
    }

    private static Document author(UUID id, String username) {
        return new Document("userId", id).append("username", username).append("fullName", null).append("avatarUrl", null);
    }

    @Test
    void profileChangeRewritesEverySnapshotOfThatUserOnly() {
        users.put(ALICE, new UserSnapshot(ALICE, "alice2", "Alice Tran", "new.png"));

        listener.onUserProfileChanged(new UserProfileChangedEvent(ALICE));

        Document alicePost = mongo.getCollection(MongoCollections.POSTS).find(new Document("author.userId", ALICE)).first();
        assertThat(alicePost.get("author", Document.class)).containsEntry("username", "alice2").containsEntry("avatarUrl", "new.png");
        List<Document> previews = alicePost.getList("previewComments", Document.class);
        assertThat(previews.get(0).get("author", Document.class)).containsEntry("username", "bob");
        assertThat(previews.get(1).get("author", Document.class)).containsEntry("username", "alice2").containsEntry("fullName", "Alice Tran");

        assertThat(mongo.getCollection(MongoCollections.COMMENTS).find().first().get("author", Document.class))
                .containsEntry("username", "alice2");
        assertThat(mongo.getCollection(MongoCollections.INBOXES).find(new Document("_id", "b:c")).first().get("peer", Document.class))
                .containsEntry("name", "Alice Tran").containsEntry("avatarUrl", "new.png");
        assertThat(mongo.getCollection(MongoCollections.INBOXES).find(new Document("_id", "a:c")).first().get("peer", Document.class))
                .containsEntry("name", "bob");
        assertThat(mongo.getCollection(MongoCollections.POSTS).find(new Document("author.userId", BOB)).first().get("author", Document.class))
                .containsEntry("username", "bob");
    }

    @Test
    void rescueCaseStatusChangeUpdatesLinkedPosts() {
        listener.onRescueCaseStatusChanged(new RescueCaseStatusChangedEvent(CASE, "IN_PROGRESS"));

        assertThat(mongo.getCollection(MongoCollections.POSTS).find(new Document("rescueCase.caseId", CASE)).first()
                .get("rescueCase", Document.class)).containsEntry("status", "IN_PROGRESS");
    }

    @Test
    void unknownUserLeavesSnapshotsUntouched() {
        listener.onUserProfileChanged(new UserProfileChangedEvent(ALICE)); // user vanished from PostgreSQL

        assertThat(mongo.getCollection(MongoCollections.POSTS).find(new Document("author.userId", ALICE)).first()
                .get("author", Document.class)).containsEntry("username", "alice");
    }
}
