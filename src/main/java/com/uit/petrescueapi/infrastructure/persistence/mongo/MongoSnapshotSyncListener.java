package com.uit.petrescueapi.infrastructure.persistence.mongo;

import com.uit.petrescueapi.application.port.out.UserSnapshotPort;
import com.uit.petrescueapi.application.port.out.UserSnapshotPort.UserSnapshot;
import com.uit.petrescueapi.domain.event.RescueCaseStatusChangedEvent;
import com.uit.petrescueapi.domain.event.UserProfileChangedEvent;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.CommentDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.InboxDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * Keeps denormalised snapshots in MongoDB in line with PostgreSQL (migrate.md §6).
 * Runs after the PostgreSQL transaction commits; always re-reads the current values, so a
 * replayed or out-of-order event converges to the right state.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoSnapshotSyncListener {

    private final MongoTemplate mongo;
    private final UserSnapshotPort userSnapshots;

    @TransactionalEventListener(fallbackExecution = true)
    public void onUserProfileChanged(UserProfileChangedEvent event) {
        resyncUser(event.userId());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onRescueCaseStatusChanged(RescueCaseStatusChangedEvent event) {
        mongo.updateMulti(query(where("rescueCase.caseId").is(event.caseId())),
                new Update().set("rescueCase.status", event.status()), PostDocument.class);
    }

    /** Rewrites every snapshot of one user from the current PostgreSQL row. */
    public void resyncUser(UUID userId) {
        UserSnapshot user = userSnapshots.findByIds(List.of(userId)).get(userId);
        if (user == null) {
            log.warn("User {} not found in PostgreSQL, snapshots left unchanged", userId);
            return;
        }
        mongo.updateMulti(query(where("author.userId").is(userId)), authorUpdate("author", user), PostDocument.class);
        mongo.updateMulti(query(where("author.userId").is(userId)), authorUpdate("author", user), CommentDocument.class);
        mongo.updateMulti(query(where("previewComments.author.userId").is(userId)),
                authorUpdate("previewComments.$[c].author", user).filterArray(where("c.author.userId").is(userId)),
                PostDocument.class);
        mongo.updateMulti(query(where("peer.userId").is(userId)),
                new Update().set("peer.name", user.displayName()).set("peer.avatarUrl", user.avatarUrl()),
                InboxDocument.class);
    }

    private static Update authorUpdate(String path, UserSnapshot user) {
        return new Update()
                .set(path + ".username", user.username())
                .set(path + ".fullName", user.fullName())
                .set(path + ".avatarUrl", user.avatarUrl());
    }
}
