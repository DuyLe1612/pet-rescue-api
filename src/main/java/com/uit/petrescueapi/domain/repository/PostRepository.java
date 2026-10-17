package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.Post;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain repository contract for the Post aggregate.
 */
public interface PostRepository {

    Post save(Post post);

    Optional<Post> findById(UUID postId);
}
