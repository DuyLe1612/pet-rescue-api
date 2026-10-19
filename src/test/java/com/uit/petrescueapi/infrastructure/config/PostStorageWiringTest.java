package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.CommentQueryDataPort;
import com.uit.petrescueapi.application.port.out.PostQueryDataPort;
import com.uit.petrescueapi.application.port.out.PostReferencePort;
import com.uit.petrescueapi.application.port.out.SocialCounterPort;
import com.uit.petrescueapi.application.port.out.UserSnapshotPort;
import com.uit.petrescueapi.domain.repository.CommentLikeRepository;
import com.uit.petrescueapi.domain.repository.CommentRepository;
import com.uit.petrescueapi.domain.repository.PostLikeRepository;
import com.uit.petrescueapi.domain.repository.PostRepository;
import com.uit.petrescueapi.infrastructure.persistence.adapter.*;
import com.uit.petrescueapi.infrastructure.persistence.mapper.CommentEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.mapper.CommentLikeEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.mapper.PostEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.mapper.PostLikeEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.mongo.adapter.*;
import com.uit.petrescueapi.infrastructure.persistence.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Exactly one implementation of every post/comment/like port is active per {@code app.storage.post}. */
class PostStorageWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    PostRepositoryAdapter.class, PostQueryAdapter.class, CommentRepositoryAdapter.class,
                    CommentQueryAdapter.class, PostLikeRepositoryAdapter.class, CommentLikeRepositoryAdapter.class,
                    SocialCounterJpaAdapter.class,
                    MongoPostRepositoryAdapter.class, MongoPostQueryAdapter.class, MongoCommentRepositoryAdapter.class,
                    MongoCommentQueryAdapter.class, MongoPostLikeRepositoryAdapter.class, MongoCommentLikeRepositoryAdapter.class,
                    MongoSocialCounterAdapter.class)
            .withBean(PostJpaRepository.class, () -> mock(PostJpaRepository.class))
            .withBean(PostQueryJpaRepository.class, () -> mock(PostQueryJpaRepository.class))
            .withBean(CommentJpaRepository.class, () -> mock(CommentJpaRepository.class))
            .withBean(CommentQueryJpaRepository.class, () -> mock(CommentQueryJpaRepository.class))
            .withBean(PostLikeJpaRepository.class, () -> mock(PostLikeJpaRepository.class))
            .withBean(CommentLikeJpaRepository.class, () -> mock(CommentLikeJpaRepository.class))
            .withBean(MediaFileJpaRepository.class, () -> mock(MediaFileJpaRepository.class))
            .withBean(PostEntityMapper.class, () -> mock(PostEntityMapper.class))
            .withBean(CommentEntityMapper.class, () -> mock(CommentEntityMapper.class))
            .withBean(PostLikeEntityMapper.class, () -> mock(PostLikeEntityMapper.class))
            .withBean(CommentLikeEntityMapper.class, () -> mock(CommentLikeEntityMapper.class))
            .withBean(CloudStoragePort.class, () -> mock(CloudStoragePort.class))
            .withBean(MongoTemplate.class, () -> mock(MongoTemplate.class))
            .withBean(UserSnapshotPort.class, () -> mock(UserSnapshotPort.class))
            .withBean(PostReferencePort.class, () -> mock(PostReferencePort.class))
            .withBean(com.uit.petrescueapi.application.port.out.PostMediaPort.class, () -> mock(com.uit.petrescueapi.application.port.out.PostMediaPort.class));

    @Test
    void postgresIsTheDefault() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(PostRepository.class)).isInstanceOf(PostRepositoryAdapter.class);
            assertThat(ctx.getBean(PostQueryDataPort.class)).isInstanceOf(PostQueryAdapter.class);
            assertThat(ctx.getBean(CommentRepository.class)).isInstanceOf(CommentRepositoryAdapter.class);
            assertThat(ctx.getBean(CommentQueryDataPort.class)).isInstanceOf(CommentQueryAdapter.class);
            assertThat(ctx.getBean(PostLikeRepository.class)).isInstanceOf(PostLikeRepositoryAdapter.class);
            assertThat(ctx.getBean(CommentLikeRepository.class)).isInstanceOf(CommentLikeRepositoryAdapter.class);
            assertThat(ctx.getBean(SocialCounterPort.class)).isInstanceOf(SocialCounterJpaAdapter.class);
        });
    }

    @Test
    void mongoModeSwapsEveryPostPort() {
        runner.withPropertyValues("app.storage.post=mongo").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(PostRepository.class)).isInstanceOf(MongoPostRepositoryAdapter.class);
            assertThat(ctx.getBean(PostQueryDataPort.class)).isInstanceOf(MongoPostQueryAdapter.class);
            assertThat(ctx.getBean(CommentRepository.class)).isInstanceOf(MongoCommentRepositoryAdapter.class);
            assertThat(ctx.getBean(CommentQueryDataPort.class)).isInstanceOf(MongoCommentQueryAdapter.class);
            assertThat(ctx.getBean(PostLikeRepository.class)).isInstanceOf(MongoPostLikeRepositoryAdapter.class);
            assertThat(ctx.getBean(CommentLikeRepository.class)).isInstanceOf(MongoCommentLikeRepositoryAdapter.class);
            assertThat(ctx.getBean(SocialCounterPort.class)).isInstanceOf(MongoSocialCounterAdapter.class);
        });
    }
}
