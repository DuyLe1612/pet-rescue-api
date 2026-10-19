package com.uit.petrescueapi.infrastructure.persistence.mongo;

/** MongoDB collection names (schema: migrate.md §2–§5). */
public final class MongoCollections {

    public static final String CONVERSATIONS = "conversations";
    public static final String INBOXES = "inboxes";
    public static final String MESSAGE_BUCKETS = "message_buckets";
    public static final String POSTS = "posts";
    public static final String COMMENTS = "comments";
    public static final String POST_LIKES = "post_likes";
    public static final String COMMENT_LIKES = "comment_likes";
    public static final String POST_ANALYSIS_RUNS = "post_analysis_runs";
    public static final String MIGRATION_STATE = "_migration_state";

    private MongoCollections() {
    }
}
