# Migrate Chat & Social Posts to MongoDB — read-optimised schema (v2)

> Status: **Implemented** (2026-10-22). Chat and posts are stored **only** in MongoDB; the
> `app.storage.*` switch and the PostgreSQL chat/post adapters are gone.
> **Decision:** existing PostgreSQL chat/post data is **not** migrated (staging has no terminal
> to run a backfill). `V25__drop_chat_post_tables.sql` drops the old tables, their data and the
> V7 trigger functions on every environment; MongoDB starts empty. §7.2–7.5 below are kept for
> reference only and were not implemented.
> Target: **MongoDB 7** (Spring Data MongoDB), same Spring Boot monolith.
> Aligned with the DA2 proposal *"Intelligent Pet Rescue and Adoption Matching System"*
> (`agent/ĐỀ-CƯƠNG-DA2-…docx`): Intelligent Post Processing (Goal 1), Explainable
> Compatibility Matching (Goal 2), end-to-end workflow (Goal 3), evaluation (Goal 4).

---

## 0. Why v2 (what was wrong with v1)

v1 copied the relational model 1:1 into Mongo — one collection per table, IDs-only references,
batch "joins" back to PostgreSQL, per-message `seen` flags. That keeps every cost of SQL and adds
the costs of NoSQL (no joins, no FKs). v2 designs each collection **for the screen that reads it**:
one indexed query per screen, no cross-store lookup on the hot path.

| Screen / operation | Today (PostgreSQL) | v1 (lift-and-shift) | **v2** |
|---|---|---|---|
| Conversation list | 4-way join (`participants` ×2, `conversations`, `users`) + Redis cache | Mongo query + PG user lookup + Redis cache | **1 indexed range scan on `inboxes`**, no cache needed |
| Open chat / scroll history | index scan of N message rows | same, N docs | **1–2 bucket documents** fetched by `_id` |
| Mark conversation read | `UPDATE chat_messages SET seen … ` (N rows) + participant update | `updateMany` N docs | **2 single-doc updates** (read watermark) |
| Send message | insert + update conversation + update each participant | same + transaction | `$inc` + 1 bucket upsert + 2 inbox updates (1 tx) |
| Feed page | join `users` + N+1 media load (`findAllById` + lazy `mediaFiles`) | Mongo + PG user lookup | **1 query, documents are render-ready** (author, media, tags, counts, preview comments, AI facets) |
| Post detail | 3 joins + 2 lazy collections | Mongo + PG lookup | **1 `_id` get** |
| Filter posts by category / species / district (DA2) | not possible (no columns) | not designed | **compound index on `facets.*`** |
| Comment thread | join `users` | Mongo + PG lookup | **1 query**, author embedded |
| Store AI output whose shape changes with the prompt version (DA2 Goal 1) | ALTER TABLE per change | — | **versioned sub-document**, no migration |

The price paid for this — and how it is controlled — is in §6 (denormalised snapshots).

---

## 1. Scope

**Moves to MongoDB:** `posts`, `post_media`, `post_tags`, `post_likes`, `comments`,
`comment_likes`, `conversations`, `conversation_participants`, `chat_messages`.

**New in MongoDB (DA2):** `post_analysis_runs` (AI audit/evaluation), optional
`match_explanations` (§5.3).

**Stays in PostgreSQL:** `users`, `organizations`, `organization_members`, `rescue_cases`,
`pets` + taxonomy, `adoption_*`, `media_files`, `tags`, `friend_requests`.
These are transactional, constraint-heavy business data; DA2 business decisions
(Rescue Case status, adoption, matching hard constraints) stay there.

**Not in scope (per DA2 §3 limitations):** no microservices — this stays one Spring Boot app
with two datastores; no semantic/personalised feed — feed is chronological + filters.

**Kept from v1:** every `_id` = the original PostgreSQL UUID (`uuid-representation=standard`),
so links and Redis like keys (`post:{id}:likes`) stay valid; domain ports
(`domain/repository/*`, `application/port/out/*`) are unchanged — only adapters are replaced;
PostgreSQL tables are archived, not dropped, until stable.

---

## 2. Chat schema

Facts from the code that shape the design:
- All conversations are **direct, 2-person** (`ChatDomainService.createDirectConversation`;
  `ConversationType` = `RESCUE | ADOPTION | GENERAL`, `relatedEntityId` = rescue case / adoption).
- Reads are always "my conversations, newest first" and "messages of one conversation, by seq".

### 2.1 `conversations` — small, write-coordination document

```js
{
  _id: UUID,
  type: "RESCUE" | "ADOPTION" | "GENERAL",
  relatedEntityId: UUID | null, relatedInfo: String | null, name: String | null,
  participantIds: [UUID, UUID],
  directKey: "RESCUE:<relatedId|->:<minUserId>:<maxUserId>",   // unique → no duplicate chats
  seq: Long,                        // last issued message seq (per conversation, contiguous)
  readSeq: { "<userId>": Long, "<userId>": Long },   // read watermark per participant
  lastMessage: { seq, messageId, senderId, preview, at },
  createdAt, updatedAt, deleted
}
```
Index: `{ directKey: 1 }` unique sparse.

### 2.2 `inboxes` — one document per (user, conversation): the conversation-list screen

```js
{
  _id: "<userId>:<conversationId>",
  userId: UUID, conversationId: UUID,
  type, relatedEntityId, relatedInfo,
  peer: { id: UUID, name: String, avatarUrl: String },   // snapshot (§6)
  lastMessage: { seq, senderId, preview, at },
  unread: Int,
  sortAt: Date,                      // = lastMessage.at, or createdAt when empty
  hidden: Boolean                    // per-user "delete conversation" later
}
```
Index: `{ userId: 1, hidden: 1, sortAt: -1, conversationId: -1 }`.

`GET /chats/conversations?cursor=` becomes:
```js
db.inboxes.find({ userId, hidden: false, sortAt: { $lt: cursor } })
          .sort({ sortAt: -1, conversationId: -1 }).limit(size)
```
Every field of `ConversationSummaryDto` is in the document → `RedisChatConversationCacheAdapter`
and `ConversationJpaRepository.findSummariesByUserCursor` (4-way native join) are deleted.

### 2.3 `message_buckets` — messages grouped 50 per document

```js
{
  _id: "<conversationId>:<bucketNo>",      // bucketNo = floor((seq - 1) / 50)
  conversationId: UUID, bucketNo: Int,
  messages: [
    { id: UUID, seq: Long, senderId: UUID, content: String, sentAt: Date,
      deleted: Boolean, deletedAt: Date }
  ]
}
```
Index: `{ "messages.id": 1 }` (delete-by-id endpoint). `_id` covers everything else.

Why buckets: a chat page is a contiguous range of `seq`, so it maps to 1–2 documents fetched by
`_id` instead of 50 index entries + 50 documents; index size shrinks ~50×. With **contiguous**
per-conversation `seq` (see §7.2 — renumbered at migration) the bucket of any message is
computable, so no lookup is needed:

```java
// GET messages before seq X (X = cursorSeq, or conversation.seq + 1 for the first page)
long hi = X - 1, lo = Math.max(1, X - size);
List<String> ids = bucketIds(convId, lo, hi);           // 1 or 2 ids
mongo.find(query(where("_id").in(ids)), MessageBucket.class)
     -> flatten, filter lo..hi, drop deleted, sort desc
// "after" direction: same with lo = X + 1, hi = X + size
```

Contiguous seq also lets the mobile client detect a gap after a WebSocket reconnect
(received 41 then 44 → fetch `after=41`).

**`seen` without per-message writes.** `ChatMessageDto.seen` is derived:
`seen = message.senderId == viewer ? message.seq <= conversation.readSeq[peer] : true`.
`ChatMessageJpaRepository.markMessagesSeen` (UPDATE of N rows) disappears.

### 2.4 Write paths

**sendMessage** (one Mongo transaction):
```js
// 1. reserve seq
c = conversations.findOneAndUpdate({_id, participantIds: sender}, {$inc:{seq:1}}, {returnNew:true})
// 2. append to bucket (upsert creates a new bucket every 50 messages)
message_buckets.updateOne({_id: convId+":"+floor((c.seq-1)/50)},
   {$push:{messages:msg}, $setOnInsert:{conversationId, bucketNo}}, {upsert:true})
// 3. conversation summary + sender read watermark
conversations.updateOne({_id}, {$set:{lastMessage, ["readSeq."+sender]: c.seq, updatedAt}})
// 4. both inboxes in one call
inboxes.bulkWrite([
  {updateOne:{filter:{_id:sender+":"+conv}, update:{$set:{lastMessage, sortAt, unread:0, hidden:false}}}},
  {updateOne:{filter:{_id:peer  +":"+conv}, update:{$set:{lastMessage, sortAt, hidden:false}, $inc:{unread:1}}}}
])
```
**markRead:** `conversations.$set readSeq.<user> = seq` + `inboxes.$set unread = 0`.
Then push a `read` socket event to the peer so their ticks update.

**deleteMessage:** `message_buckets.updateOne({"messages.id": id}, {$set:{"messages.$.deleted":true,…}})`;
if it was `lastMessage`, recompute preview from the same bucket and update conversation + both inboxes.

**createDirectConversation:** insert `conversations` with `directKey`; on duplicate-key error,
return the existing one (fixes today's race). Insert both `inboxes` in the same transaction.

---

## 3. Social post schema (with DA2 Intelligent Post Processing)

### 3.1 `posts` — render-ready feed document

```js
{
  _id: UUID,
  author: { id: UUID, username: String, fullName: String, avatarUrl: String },  // snapshot
  content: String,
  media: [ { mediaId: UUID, publicId: String, kind: "IMAGE" } ],   // from post_media ⨝ media_files
  tags:  [ String ],                     // tag codes, user + accepted AI tags
  stats: { likes: Int, comments: Int },
  previewComments: [                     // newest ≤ 2 root comments, for the feed card
     { id: UUID, author: { id, username, avatarUrl }, content: String, createdAt: Date } ],

  // ── DA2 Goal 1: Intelligent Post Processing ────────────────────────
  analysis: {
    status: "PENDING" | "ANALYZED" | "CONFIRMED" | "REJECTED" | "FAILED",
    schemaVersion: 1,                    // AI output contract version
    model: String, analyzedAt: Date, lastRunId: UUID,   // -> post_analysis_runs
    suggested: {                         // validated LLM output, never trusted blindly
      category: "RESCUE" | "ADOPTION" | "LOST" | "FOUND" | "GENERAL",
      species: "DOG" | "CAT" | …,         // must exist in pet taxonomy (PostgreSQL)
      location: { text: "Q7", district: "Quận 7", province: "TP. Hồ Chí Minh" },
      rescueContext: { needsRescue: true, summary: "Possible leg injury", urgency: "HIGH" },
      tags: [ "injured", "dog" ],
      confidence: { category: 0.93, species: 0.97, location: 0.88 }
    },
    confirmedBy: UUID, confirmedAt: Date
  },
  facets: {                              // values the USER accepted/edited → used for filtering
    category: "RESCUE", species: "DOG", district: "Quận 7", province: "TP. Hồ Chí Minh",
    needsRescue: true
  },
  rescueCase: { id: UUID, status: "OPEN" } | null,   // set when a Rescue Case is created from the post
  rescueSuggestion: "NONE" | "SUGGESTED" | "ACCEPTED" | "DISMISSED",

  createdAt: Date, updatedAt: Date, deleted: Boolean, deletedAt: Date, deletedBy: UUID
}
```

Indexes:
```js
db.posts.createIndex({ deleted: 1, createdAt: -1, _id: -1 })                        // feed
db.posts.createIndex({ "facets.category": 1, "facets.species": 1, "facets.district": 1, createdAt: -1, _id: -1 },
                     { partialFilterExpression: { deleted: false } })               // DA2 search & filter
db.posts.createIndex({ tags: 1, createdAt: -1 })
db.posts.createIndex({ "author.id": 1, createdAt: -1 })
db.posts.createIndex({ "analysis.status": 1, createdAt: 1 })                         // AI worker retry queue
db.posts.createIndex({ "rescueCase.id": 1 }, { sparse: true })
db.posts.createIndex({ content: "text" }, { default_language: "none" })              // Vietnamese: no stemming
```

**Why `suggested` vs `facets`:** DA2 states *"AI chỉ đóng vai trò phân tích và đề xuất"* — the
AI never writes the searchable/business fields directly. `facets` is filled only on user
confirmation (or by an org staff/admin), which is also what makes Goal 4 measurable (§5.2).

### 3.2 `comments`

```js
{
  _id: UUID, postId: UUID,
  parentId: UUID | null,                 // null = root; replies point at a root (2 levels)
  author: { id, username, avatarUrl },   // snapshot
  content: String,
  stats: { likes: Int, replies: Int },
  createdAt, updatedAt, deleted, deletedAt, deletedBy
}
```
Index: `{ postId: 1, parentId: 1, createdAt: -1, _id: -1 }`.
Two-level rule (was the V7 trigger) moves to `CommentDomainService`.
Replies are separate documents (unbounded); roots are not embedded in the post except the
2 `previewComments` for the feed card.

### 3.3 Likes

`post_likes { _id: { postId, userId }, at }`, `comment_likes { _id: { commentId, userId }, at }`
(compound `_id` = idempotent like). Counters live in `posts.stats.likes` /
`comments.stats.likes`, updated with `$inc` by `PostLikeEventListener` / `CommentEventListener`
only — this removes today's **double counting** (V7 triggers *and* listeners both increment).
"Liked by me" for a feed page: one query
`post_likes.find({ _id: { $in: [ {postId:p1,userId:me}, … ] } })` or the existing Redis zsets.

### 3.4 Feed query (also fixes the cursor bug)

Today `PostQueryAdapter.findFeedByCursor` passes `viewerId` as `cursorPostId`, and the response
only returns `nextCursor` (time). v2 cursor = `(createdAt, _id)` both returned:

```js
db.posts.find({ deleted: false,
                $or: [ { createdAt: { $lt: t } }, { createdAt: t, _id: { $lt: id } } ],
                // optional DA2 filters
                "facets.category": "RESCUE", "facets.species": "DOG" })
        .sort({ createdAt: -1, _id: -1 }).limit(size)
```
No media/author/tags follow-up queries — `PostQueryAdapter`'s batch `findAllById` +
lazy `getMediaFiles()` is removed. Image URLs are still built at read time with
`CloudStoragePort.buildUrl(publicId)`.

---

## 4. Flexible-schema rules (keep NoSQL from becoming a mess)

1. Every collection has a Java `@Document` class; no `Map<String,Object>` in domain code.
2. `analysis.schemaVersion` is bumped when the LLM output contract changes; readers handle
   old versions (or a backfill rewrites them). No ALTER TABLE, but also no silent drift.
3. MongoDB JSON-Schema validators on `posts`, `message_buckets`, `inboxes`
   (`validationLevel: "moderate"`) for required fields and enum values.
4. Array growth is bounded: `messages` ≤ 50 per bucket, `previewComments` ≤ 2
   (`$push` with `$slice: -2`), `media` ≤ 10 (validated in `PostDomainService`).

---

## 5. DA2 integration

### 5.1 Goal 1 + 3 — Post → AI Post Processing → Rescue Case

```
POST /posts ─► PostDomainService.create  (analysis.status = PENDING)
           └─► PostCreatedEvent (RabbitMQ, existing EventPublisher)
                     │
                     ▼
     PostAnalysisWorker (@RabbitListener)
       1. build prompt from content (+ taxonomy list from PostgreSQL)
       2. LLM call via PostAnalysisPort with a fixed JSON schema (structured output)
       3. validate: enums, species ∈ taxonomy, district ∈ known list, confidence ∈ [0,1]
       4. insert post_analysis_runs (raw + parsed + validation errors)
       5. posts.$set analysis.{status: ANALYZED, suggested, …}, rescueSuggestion
          (FAILED + retry with backoff on error; index on analysis.status serves the retry scan)
                     │
                     ▼
     Author sees chips "RESCUE · DOG · Quận 7 · needs rescue?"  → edits/accepts
       PATCH /posts/{id}/analysis/confirm  → facets.*, tags, analysis.status = CONFIRMED
                     │  (if facets.needsRescue)
                     ▼
       POST /posts/{id}/rescue-case  → RescueCaseDomainService (PostgreSQL, existing rules)
                                       prefilled from facets; then posts.$set rescueCase {id,status}
       RescueCase status changes → RescueCaseStatusChangedEvent → posts.updateOne(rescueCase.status)
```

`post_analysis_runs`:
```js
{ _id: UUID, postId: UUID, model: String, promptVersion: Int, schemaVersion: Int,
  input: String, rawOutput: String, parsed: {...}, validationErrors: [String],
  latencyMs: Int, tokens: { in: Int, out: Int }, createdAt: Date,
  humanLabel: {...} | null }        // copied from facets on confirm → ground truth for Goal 4
```
Index: `{ postId: 1, createdAt: -1 }`, `{ promptVersion: 1, createdAt: -1 }`.
TTL is **not** set — runs are the evaluation dataset.

### 5.2 Goal 4 — evaluation straight from Mongo

Because AI output (`parsed`) and the user-confirmed label (`humanLabel`) are stored side by side,
accuracy per field and per prompt version is one aggregation:

```js
db.post_analysis_runs.aggregate([
  { $match: { humanLabel: { $ne: null } } },
  { $group: { _id: "$promptVersion",
      n: { $sum: 1 },
      categoryOk: { $sum: { $cond: [{ $eq: ["$parsed.category", "$humanLabel.category"] }, 1, 0] } },
      speciesOk:  { $sum: { $cond: [{ $eq: ["$parsed.species",  "$humanLabel.species"]  }, 1, 0] } },
      districtOk: { $sum: { $cond: [{ $eq: ["$parsed.location.district", "$humanLabel.district"] }, 1, 0] } },
      rescueOk:   { $sum: { $cond: [{ $eq: ["$parsed.rescueContext.needsRescue", "$humanLabel.needsRescue"] }, 1, 0] } },
      invalid:    { $sum: { $cond: [{ $gt: [{ $size: "$validationErrors" }, 0] }, 1, 0] } } } }
])
```
Expose it as an admin endpoint (`AdminController`) for the report.

### 5.3 Goal 2 — Compatibility Matching (stays in PostgreSQL)

Hard constraints and weighted scoring work on pets, adopter profiles and adoption rules — all
relational, all in PostgreSQL; this migration does not move them. Optional Mongo use:
`match_explanations { _id: "<adopterId>:<petId>", score, level: "HIGH", weightsVersion,
reasons: [ { code, ok: true|false|"warn", text } ], computedAt }` with a TTL index on
`computedAt` (cache of explanation output, recomputable, reproducible via `weightsVersion`).
Decide when Goal 2 is designed; not required for this migration.

### 5.4 Chat ↔ DA2 workflow

`RESCUE` conversations reference `rescue_cases.case_id`, `ADOPTION` ones an adoption
application. The inbox already carries `type` + `relatedInfo`, so the app can show
"Rescue: Chó bị thương Q7" without a lookup.

---

## 6. Denormalised snapshots — the cost and how it is paid

| Snapshot | Copied from (PostgreSQL) | Updated when | Mechanism |
|---|---|---|---|
| `posts.author`, `comments.author`, `posts.previewComments.author` | `users` | username / full name / avatar changes | `UserProfileUpdatedEvent` (new, published by user command side) → listener `updateMany({"author.id": id})` on each collection |
| `inboxes.peer` | `users` | same event | `updateMany({"peer.id": id})` |
| `posts.media[].publicId` | `media_files` | never (Cloudinary `public_id` is immutable) | — |
| `posts.tags` (codes) | `tags` | tag code renamed (admin, rare) | `updateMany({tags: old}, {$set: {"tags.$": new}})` |
| `posts.rescueCase.status` | `rescue_cases` | status change | `RescueCaseStatusChangedEvent` listener |
| `inboxes.lastMessage`, `unread` | `conversations` / buckets | every message | same transaction as the message (§2.4) |

Profile edits are rare compared with feed/chat reads, so moving cost from every read to the rare
write is the intended trade. A nightly reconciler (`@Scheduled`) re-syncs author snapshots
changed in the last 24 h in case an event was lost.

---

## 7. Migration from existing PostgreSQL data

### 7.1 Infrastructure (unchanged from v1)

- `spring-boot-starter-data-mongodb`, `testcontainers:mongodb`.
- `spring.data.mongodb.uri=…?replicaSet=rs0`, `spring.data.mongodb.uuid-representation=standard`,
  `spring.data.mongodb.auto-index-creation=false`; indexes + validators created by a
  `MongoSchemaInitializer` `ApplicationRunner`.
- docker-compose: `mongo:7` with `--replSet rs0` and a healthcheck that runs `rs.initiate`
  (single-node replica set is required for transactions).
- `MongoTransactionManager` bean; JPA manager stays `@Primary`. Chat/Post/Comment domain
  services use `@Transactional("mongoTransactionManager")` — a plain `@Transactional` would open a
  JPA transaction and leave Mongo writes non-atomic.
- Adapters switched with `@ConditionalOnProperty(app.storage.chat|post = postgres|mongo)`.

### 7.2 Backfill job (`--spring.profiles.active=migrate-nosql`)


Reads PostgreSQL with `JdbcTemplate` (keyset on PK, batch 1000), writes with
`BulkOperations` upserts, stores progress in `_migration_state` so it can resume.

**Chat**
```sql
-- renumber seq per conversation: old global BIGSERIAL → contiguous 1..n
SELECT m.*, ROW_NUMBER() OVER (PARTITION BY m.conversation_id ORDER BY m.message_seq) AS new_seq
FROM chat_messages m
WHERE m.conversation_id = ANY(:conversationBatch)
ORDER BY m.conversation_id, new_seq;
```
- Group rows into buckets `floor((new_seq-1)/50)` → `message_buckets`.
- `conversations.seq = max(new_seq)`; `lastMessage` = highest non-deleted message.
- `readSeq[user]` = highest `new_seq` with `sent_at <= participant.last_read_at`
  (0 if `last_read_at` is null). The old per-message `seen` column is dropped — it is now derived.
- `directKey` from type/related id/sorted participant ids. Duplicates produced by the old race:
  keep the conversation with more messages, merge the other's messages into it (re-sequenced by
  `sent_at`), log both ids.
- `inboxes`: one per participant; `peer` from `users` (`COALESCE(NULLIF(full_name,''), username)`,
  `avatar_url`); `unread` from `conversation_participants.unread_count`.

**Posts**
```sql
SELECT p.*, u.username, u.full_name, u.avatar_url,
       COALESCE(json_agg(DISTINCT jsonb_build_object('mediaId', m.media_id, 'publicId', m.public_id))
                FILTER (WHERE m.media_id IS NOT NULL), '[]') AS media,
       COALESCE(array_agg(DISTINCT t.code) FILTER (WHERE t.code IS NOT NULL), '{}') AS tags
FROM posts p
LEFT JOIN users u        ON u.user_id = p.author_id
LEFT JOIN post_media pm  ON pm.post_id = p.post_id
LEFT JOIN media_files m  ON m.media_id = pm.media_id
LEFT JOIN post_tags pt   ON pt.post_id = p.post_id
LEFT JOIN tags t         ON t.tag_id   = pt.tag_id
WHERE p.post_id > :lastId
GROUP BY p.post_id, u.user_id
ORDER BY p.post_id LIMIT 1000;
```
- `rescueCase` snapshot from `rescue_cases` when `rescue_case_id` is set;
  existing posts with a rescue case get `facets.category = RESCUE`, `facets.needsRescue = true`.
- `analysis.status = PENDING` for all migrated posts → after cutover the worker analyses old
  posts too (rate-limited), which also seeds the Goal 4 dataset.
- Comments → `comments` with author snapshot; likes → `post_likes` / `comment_likes`.
- **Recompute** `stats.likes`, `stats.comments`, `stats.replies` from the like/comment
  collections with `$group` + `$merge` (stored counters are inflated by the double counting).
- `previewComments` = 2 newest non-deleted root comments per post (`$sort` + `$group` + `$slice` + `$merge`).
- Timestamps: `TIMESTAMPTZ` → UTC `Instant`, truncated to ms.

### 7.3 Verification (abort cutover on failure)

1. Counts: posts, comments, likes, conversations, sum of bucket message counts = `chat_messages` rows, inboxes = participants rows.
2. Sample 500 ids per entity, field-by-field compare.
3. Per conversation: seqs are exactly `1..conversation.seq`, each in the bucket its formula predicts.
4. 50 random users: conversation list order and feed order match a PostgreSQL-mode instance on frozen data.

### 7.4 Cutover (maintenance window)

1. Stop app, drain RabbitMQ like/comment queues, `pg_dump`.
2. Run backfill → verifier.
3. Delete Redis chat-conversation cache keys (adapter is removed anyway).
4. Start with `app.storage.chat=mongo`, `app.storage.post=mongo`.
5. **Client note:** message `seq` values are renumbered. The mobile app must drop any locally
   cached `cursorSeq` / message lists on the new version (bump a cache key / force refresh) —
   message ids are unchanged, only seq.
6. Smoke test: feed + filters, post detail, comment/reply, like/unlike, conversation list,
   send/read/delete over WebSocket, scroll history across a bucket boundary.

### 7.5 Rollback & cleanup

- Rollback window ≥ 2 weeks: PostgreSQL tables kept read-only; reverse export job
  (Mongo → PG upsert by id) carries over new rows if rolling back.
- Then: delete JPA entities/repositories/adapters/mappers for moved tables,
  `RedisChatConversationCacheAdapter`, `ChatConversationCachePort`; new Flyway migration at the next
  free version (e.g. `V25__archive_chat_post_tables.sql`, never reuse a used number) renames tables
  to `_archived_*` and drops the V7 trigger functions; a later migration drops them.

---

## 8. Code changes (by layer)

| Layer | Change |
|---|---|
| domain | `ChatMessage`: `seen` becomes derived; `Post`: add `analysis`, `facets`, `rescueSuggestion`; new events `PostCreatedEvent`, `UserProfileUpdatedEvent`, `RescueCaseStatusChangedEvent`; nesting rule + media limit in domain services; `@Transactional("mongoTransactionManager")` |
| application | new ports `PostAnalysisPort` (LLM), `PostAnalysisRunRepository`; use cases `ConfirmPostAnalysis`, `CreateRescueCaseFromPost`; `PostCursorResponseDto` gets `nextCursorId`; post search DTO gets `category/species/district` filters |
| infrastructure | Mongo `@Document`s + adapters for every port in §1; `PostAnalysisWorker`; snapshot sync listeners; `MongoSchemaInitializer`; backfill + verifier under `migrate-nosql` profile |
| presentation | `GET /posts?category=&species=&district=&tag=`, `PATCH /posts/{id}/analysis/confirm`, `POST /posts/{id}/rescue-case`, `GET /admin/ai/post-analysis/metrics`; chat endpoints unchanged (response adds nothing; `seen` still present) |
| tests | Testcontainers Mongo; contract tests per port (JPA vs Mongo); backfill fixture (gaps in seq, duplicate direct chat, deleted rows, post without media); bucket-boundary paging; AI output validation tests with malformed JSON |

---

## 9. Checklist

- [x] Mongo replica set in docker-compose (dev) and prod compose (auth)
- [x] Dependency, config, `MongoTransactionManager`, `MongoSchemaInitializer` (indexes + validators)
- [x] Chat: `conversations`, `inboxes`, `message_buckets` adapters; derived `seen`; unique `directKey`
- [x] Posts: `posts`, `comments`, likes adapters; feed cursor fix; facet filters
- [x] Snapshot sync events + nightly reconciler
- [ ] DA2 Goal 1: `PostAnalysisPort`, worker, validation, `post_analysis_runs`, confirm + create-rescue-case endpoints
- [ ] DA2 Goal 4: evaluation aggregation endpoint
- [x] ~~Backfill + verifier~~ dropped: old data is deleted instead (V25)
- [ ] Mobile app: invalidate cached chat cursors on release
- [x] Remove JPA chat/post code and the `app.storage.*` switch
- [x] Drop the PostgreSQL chat/post tables (V25)
- [ ] Smoke test on staging
