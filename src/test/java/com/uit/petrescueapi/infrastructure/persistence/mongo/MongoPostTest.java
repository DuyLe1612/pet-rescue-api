package com.uit.petrescueapi.infrastructure.persistence.mongo;

import com.uit.petrescueapi.application.dto.comment.CommentSummaryDto;
import com.uit.petrescueapi.application.dto.post.PostCursorResponseDto;
import com.uit.petrescueapi.application.dto.post.PostFeedFilter;
import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.application.dto.post.PostSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.CommentQueryDataPort;
import com.uit.petrescueapi.application.port.out.PostQueryDataPort;
import com.uit.petrescueapi.application.port.out.PostReferencePort;
import com.uit.petrescueapi.application.port.out.SocialCounterPort;
import com.uit.petrescueapi.application.port.out.UserSnapshotPort;
import com.uit.petrescueapi.application.port.out.UserSnapshotPort.UserSnapshot;
import com.uit.petrescueapi.domain.entity.Comment;
import com.uit.petrescueapi.domain.entity.Post;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.service.CommentDomainService;
import com.uit.petrescueapi.domain.service.LikeDomainService;
import com.uit.petrescueapi.domain.service.PostDomainService;
import com.uit.petrescueapi.infrastructure.config.MongoConfig;
import com.uit.petrescueapi.infrastructure.config.StorageTransactionAliases;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.*;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.AuthorDocument;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.PostDocument;
import com.uit.petrescueapi.support.MongoIntegrationTest;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Import(MongoPostTest.Config.class)
@TestPropertySource(properties = "app.storage.post=mongo")
class MongoPostTest extends MongoIntegrationTest {

    static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    static final UUID MEDIA_1 = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    static final UUID MEDIA_2 = UUID.fromString("00000000-0000-0000-0000-0000000000e2");
    static final UUID RESCUE = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    @TestConfiguration
    @Import({MongoPostRepositoryAdapter.class, MongoPostQueryAdapter.class, MongoCommentRepositoryAdapter.class,
            MongoCommentQueryAdapter.class, MongoPostLikeRepositoryAdapter.class, MongoCommentLikeRepositoryAdapter.class,
            MongoSocialCounterAdapter.class, PostDomainService.class, CommentDomainService.class, LikeDomainService.class,
            MongoTransientRetryAspect.class})
    @EnableTransactionManagement
    @EnableAspectJAutoProxy
    static class Config {
        @Bean(MongoConfig.MONGO_TX)
        MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory f) {
            return new MongoTransactionManager(f);
        }

        @Bean
        static StorageTransactionAliases storageTransactionAliases() {
            return new StorageTransactionAliases();
        }

        @Bean
        UserSnapshotPort userSnapshotPort() {
            Map<UUID, UserSnapshot> users = List.of(
                    new UserSnapshot(ALICE, "alice", "Alice Nguyen", "a.png"),
                    new UserSnapshot(BOB, "bob", null, "b.png")
            ).stream().collect(Collectors.toMap(UserSnapshot::id, Function.identity()));
            return ids -> ids.stream().filter(users::containsKey).collect(Collectors.toMap(Function.identity(), users::get));
        }

        @Bean
        PostReferencePort postReferencePort() {
            return new PostReferencePort() {
                @Override
                public Map<UUID, String> findMediaPublicIds(Collection<UUID> mediaIds) {
                    Map<UUID, String> known = Map.of(MEDIA_1, "posts/one", MEDIA_2, "posts/two");
                    return mediaIds.stream().filter(known::containsKey).collect(Collectors.toMap(Function.identity(), known::get));
                }

                @Override
                public Set<String> findExistingTagCodes(Collection<String> codes) {
                    return codes.stream().filter(Set.of("rescue", "dog", "cat")::contains).collect(Collectors.toSet());
                }

                @Override
                public Optional<String> findRescueCaseStatus(UUID caseId) {
                    return RESCUE.equals(caseId) ? Optional.of("OPEN") : Optional.empty();
                }
            };
        }

        @Bean
        CloudStoragePort cloudStoragePort() {
            CloudStoragePort port = mock(CloudStoragePort.class);
            when(port.buildUrl(anyString())).thenAnswer(inv -> "https://cdn/" + inv.getArgument(0));
            return port;
        }
    }

    @Autowired MongoTemplate mongo;
    @Autowired PostDomainService posts;
    @Autowired CommentDomainService comments;
    @Autowired LikeDomainService likes;
    @Autowired PostQueryDataPort postQuery;
    @Autowired CommentQueryDataPort commentQuery;
    @Autowired SocialCounterPort counters;

    @BeforeEach
    void clean() {
        mongo.getDb().drop();
        new MongoSchemaInitializer(mongo).run(null);
    }

    private Post newPost(UUID author, String content) {
        return posts.create(Post.builder().authorId(author).content(content).build());
    }

    private Comment comment(UUID postId, UUID parentId, UUID author, String content) {
        return comments.createComment(Comment.builder().postId(postId).parentCommentId(parentId).authorId(author).content(content).build());
    }

    private Document raw(UUID postId) {
        return mongo.getCollection(MongoCollections.POSTS).find(new Document("_id", postId)).first();
    }

    // ── create / detail ──────────────────────────────────────

    @Test
    void createdPostIsRenderReadyWithAuthorMediaTagsAndRescueCase() {
        Post p = posts.create(Post.builder().authorId(ALICE).content("Bé cún bị thương ở Q7")
                .mediaIds(List.of(MEDIA_2, MEDIA_1)).tagCodes(List.of("rescue", "dog")).rescueCaseId(RESCUE).build());

        PostResponseDto dto = postQuery.findById(p.getPostId());
        assertThat(dto.getAuthorUsername()).isEqualTo("alice");
        assertThat(dto.getImageUrls()).containsExactly("https://cdn/posts/two", "https://cdn/posts/one"); // upload order kept
        assertThat(dto.getTags()).containsExactly("rescue", "dog");
        assertThat(dto.getRescueCaseId()).isEqualTo(RESCUE);

        Document doc = raw(p.getPostId());
        assertThat(doc.get("rescueCase", Document.class).getString("status")).isEqualTo("OPEN");
        assertThat(doc.get("stats", Document.class)).containsEntry("likes", 0).containsEntry("comments", 0);
    }

    @Test
    void rejectsUnknownMediaTagsAndRescueCase() {
        assertThatThrownBy(() -> posts.create(Post.builder().authorId(ALICE).content("x").mediaIds(List.of(UUID.randomUUID())).build()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> posts.create(Post.builder().authorId(ALICE).content("x").tagCodes(List.of("nope")).build()))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> posts.create(Post.builder().authorId(ALICE).content("x").rescueCaseId(UUID.randomUUID()).build()))
                .isInstanceOf(BusinessException.class);
        assertThat(mongo.getCollection(MongoCollections.POSTS).countDocuments()).isZero();
    }

    @Test
    void updateChangesContentAndMediaButKeepsAuthorAndStats() {
        Post p = newPost(ALICE, "v1");
        counters.addPostLikes(p.getPostId(), 1);

        posts.update(p.getPostId(), Post.builder().content("v2").mediaIds(List.of(MEDIA_1)).build());

        PostResponseDto dto = postQuery.findById(p.getPostId());
        assertThat(dto.getContent()).isEqualTo("v2");
        assertThat(dto.getImageUrls()).containsExactly("https://cdn/posts/one");
        assertThat(dto.getAuthorUsername()).isEqualTo("alice");
        assertThat(dto.getUpdatedAt()).isNotNull();
        assertThat(raw(p.getPostId()).get("stats", Document.class)).containsEntry("likes", 1);
    }

    @Test
    void deletedPostIsHiddenFromDetailAndFeed() {
        Post p = newPost(ALICE, "bye");
        posts.delete(p.getPostId());

        assertThatThrownBy(() -> postQuery.findById(p.getPostId())).isInstanceOf(RuntimeException.class);
        assertThat(postQuery.findFeedByCursor(null, null, 10, null, PostFeedFilter.none()).getItems()).isEmpty();
    }

    // ── feed ─────────────────────────────────────────────────

    private void insertPost(UUID id, LocalDateTime createdAt, List<String> tags, String category) {
        PostDocument doc = PostDocument.builder()
                .id(id)
                .author(AuthorDocument.builder().userId(ALICE).username("alice").build())
                .content("p " + id)
                .tags(tags)
                .facets(category == null ? null : PostDocument.FacetsDocument.builder().category(category).species("DOG").district("Quận 7").build())
                .createdAt(createdAt)
                .build();
        mongo.insert(doc);
    }

    @Test
    void feedCursorUsesCreatedAtAndIdSoTiesAreNeitherSkippedNorRepeated() {
        LocalDateTime t = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UUID id = UUID.randomUUID();
            ids.add(id);
            insertPost(id, i < 4 ? t : t.minusSeconds(1), List.of(), null); // 4 posts share one timestamp
        }

        List<UUID> seen = new ArrayList<>();
        PostCursorResponseDto page = postQuery.findFeedByCursor(null, null, 2, null, PostFeedFilter.none());
        seen.addAll(page.getItems().stream().map(PostSummaryResponseDto::getPostId).toList());
        while (page.isHasMore()) {
            page = postQuery.findFeedByCursor(page.getNextCursor(), page.getNextCursorId(), 2, null, PostFeedFilter.none());
            seen.addAll(page.getItems().stream().map(PostSummaryResponseDto::getPostId).toList());
        }
        assertThat(seen).hasSize(5).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    void feedFiltersByTagAndFacets() {
        LocalDateTime t = LocalDateTime.now();
        UUID rescueDog = UUID.randomUUID();
        UUID taggedOnly = UUID.randomUUID();
        insertPost(rescueDog, t, List.of("dog"), "RESCUE");
        insertPost(taggedOnly, t.minusSeconds(1), List.of("dog"), null);
        insertPost(UUID.randomUUID(), t.minusSeconds(2), List.of("cat"), "ADOPTION");

        assertThat(postQuery.findFeedByCursor(null, null, 10, null, new PostFeedFilter(null, null, null, "dog")).getItems())
                .extracting(PostSummaryResponseDto::getPostId).containsExactly(rescueDog, taggedOnly);
        assertThat(postQuery.findFeedByCursor(null, null, 10, null, new PostFeedFilter("RESCUE", "DOG", "Quận 7", null)).getItems())
                .extracting(PostSummaryResponseDto::getPostId).containsExactly(rescueDog);
    }

    @Test
    void feedItemsCarryImagesAndPreviewCommentsWithoutExtraLookups() {
        Post p = posts.create(Post.builder().authorId(ALICE).content("feed").mediaIds(List.of(MEDIA_1)).build());
        comment(p.getPostId(), null, BOB, "first");
        comment(p.getPostId(), null, BOB, "second");
        comment(p.getPostId(), null, ALICE, "third");

        PostSummaryResponseDto item = postQuery.findFeedByCursor(null, null, 10, null, PostFeedFilter.none()).getItems().get(0);
        assertThat(item.getImageUrls()).containsExactly("https://cdn/posts/one");
        assertThat(item.getAuthorAvatarUrl()).isEqualTo("a.png");
        assertThat(item.getPreviewComments()).extracting("content").containsExactly("third", "second");
    }

    @Test
    void searchMatchesContentOrAuthorCaseInsensitiveAndTreatsInputLiterally() {
        newPost(ALICE, "Found a CAT near the market");
        newPost(BOB, "Lost dog (brown)");
        newPost(BOB, "nothing here");

        assertThat(postQuery.findAllSummaries("cat", PageRequest.of(0, 10, Sort.by("createdAt").descending())).getContent())
                .extracting(PostSummaryResponseDto::getContent).containsExactly("Found a CAT near the market");
        assertThat(postQuery.findAllSummaries("ALICE", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(postQuery.findAllSummaries("(brown)", PageRequest.of(0, 10)).getContent()).hasSize(1);
        assertThat(postQuery.findAllSummaries(null, PageRequest.of(0, 2)).getTotalElements()).isEqualTo(3);
    }

    // ── comments ─────────────────────────────────────────────

    @Test
    void repliesCannotBeNested() {
        Post p = newPost(ALICE, "post");
        Comment root = comment(p.getPostId(), null, BOB, "root");
        Comment reply = comment(p.getPostId(), root.getCommentId(), ALICE, "reply");
        assertThatThrownBy(() -> comment(p.getPostId(), reply.getCommentId(), BOB, "too deep")).isInstanceOf(BusinessException.class);
    }

    @Test
    void parentCommentsPageNewestFirstAndRepliesOldestFirst() throws Exception {
        Post p = newPost(ALICE, "post");
        List<Comment> roots = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            roots.add(comment(p.getPostId(), null, BOB, "root" + i));
            Thread.sleep(3);
        }
        comment(p.getPostId(), roots.get(0).getCommentId(), ALICE, "r1");
        Thread.sleep(3);
        comment(p.getPostId(), roots.get(0).getCommentId(), BOB, "r2");

        var first = commentQuery.findParentCommentsByPostId(p.getPostId(), null, PageRequest.of(0, 2));
        assertThat(first.getContent()).extracting(CommentSummaryDto::getContent).containsExactly("root2", "root1");
        assertThat(first.hasNext()).isTrue();
        var second = commentQuery.findParentCommentsByPostId(p.getPostId(), first.getContent().get(1).getCreatedAt(), PageRequest.of(0, 2));
        assertThat(second.getContent()).extracting(CommentSummaryDto::getContent).containsExactly("root0");
        assertThat(second.hasNext()).isFalse();

        assertThat(commentQuery.findRepliesByParentCommentId(roots.get(0).getCommentId(), PageRequest.of(0, 10)).getContent())
                .extracting(CommentSummaryDto::getContent, CommentSummaryDto::getAuthorUsername)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("r1", "alice"), org.assertj.core.groups.Tuple.tuple("r2", "bob"));
    }

    @Test
    void deletingCommentsDecrementsCountersAndRefreshesPreview() {
        Post p = newPost(ALICE, "post");
        Comment a = comment(p.getPostId(), null, BOB, "a");
        Comment b = comment(p.getPostId(), null, BOB, "b");
        Comment reply = comment(p.getPostId(), a.getCommentId(), ALICE, "reply");
        // counters are incremented asynchronously by the RabbitMQ listeners in the app
        counters.addPostComments(p.getPostId(), 3);
        counters.addCommentReplies(a.getCommentId(), 1);

        comments.deleteComment(reply.getCommentId());
        comments.deleteComment(b.getCommentId());
        comments.deleteComment(b.getCommentId()); // idempotent: no double decrement

        assertThat(raw(p.getPostId()).get("stats", Document.class)).containsEntry("comments", 1);
        assertThat(commentQuery.findById(a.getCommentId()).getReplyCount()).isZero();
        assertThat(postQuery.findFeedByCursor(null, null, 10, null, PostFeedFilter.none()).getItems().get(0).getPreviewComments())
                .extracting("content").containsExactly("a");
    }

    // ── likes / counters ─────────────────────────────────────

    @Test
    void likesAreIdempotentPerUser() {
        Post p = newPost(ALICE, "post");
        assertThat(likes.likePost(p.getPostId(), BOB)).isTrue();
        assertThat(likes.likePost(p.getPostId(), BOB)).isFalse();
        assertThat(likes.unlikePost(p.getPostId(), BOB)).isTrue();
        assertThat(likes.unlikePost(p.getPostId(), BOB)).isFalse();

        Comment c = comment(p.getPostId(), null, BOB, "c");
        assertThat(likes.likeComment(c.getCommentId(), ALICE)).isTrue();
        assertThat(likes.likeComment(c.getCommentId(), ALICE)).isFalse();
    }

    @Test
    void countersNeverGoNegative() {
        Post p = newPost(ALICE, "post");
        counters.addPostLikes(p.getPostId(), 1);
        counters.addPostLikes(p.getPostId(), -1);
        counters.addPostLikes(p.getPostId(), -1);

        assertThat(postQuery.findFeedByCursor(null, null, 10, null, PostFeedFilter.none()).getItems().get(0).getLikeCount()).isZero();
    }
}
