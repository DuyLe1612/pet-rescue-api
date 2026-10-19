package com.uit.petrescueapi.infrastructure.persistence.mongo.adapter;

import com.uit.petrescueapi.application.dto.comment.CommentResponseDto;
import com.uit.petrescueapi.application.dto.comment.CommentSummaryDto;
import com.uit.petrescueapi.application.port.out.CommentQueryDataPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.infrastructure.config.PostOnMongo;
import com.uit.petrescueapi.infrastructure.persistence.mongo.document.CommentDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/** Comment threads from MongoDB — author is embedded, so no user join. */
@Component
@PostOnMongo
@RequiredArgsConstructor
public class MongoCommentQueryAdapter implements CommentQueryDataPort {

    private final MongoTemplate mongo;

    @Override
    public Page<CommentSummaryDto> findParentCommentsByPostId(UUID postId, LocalDateTime cursor, Pageable pageable) {
        Criteria criteria = where("postId").is(postId).and("parentId").is(null).and("deleted").is(false);
        if (cursor != null) {
            criteria = criteria.and("createdAt").lt(cursor);
        }
        int size = pageable.getPageSize();
        // fetch one extra row to know whether another page exists, without a count query
        List<CommentDocument> rows = mongo.find(query(criteria)
                        .with(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("_id")))
                        .skip(pageable.getOffset())
                        .limit(size + 1),
                CommentDocument.class);
        boolean more = rows.size() > size;
        List<CommentSummaryDto> items = rows.stream().limit(size).map(MongoCommentQueryAdapter::toSummary).toList();
        return new PageImpl<>(items, pageable, pageable.getOffset() + items.size() + (more ? 1 : 0));
    }

    @Override
    public Page<CommentSummaryDto> findRepliesByParentCommentId(UUID parentCommentId, Pageable pageable) {
        Criteria criteria = where("parentId").is(parentCommentId).and("deleted").is(false);
        long total = mongo.count(query(criteria), CommentDocument.class);
        Query q = query(criteria)
                .with(Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("_id")))
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize());
        List<CommentSummaryDto> items = mongo.find(q, CommentDocument.class).stream()
                .map(MongoCommentQueryAdapter::toSummary).toList();
        return new PageImpl<>(items, pageable, total);
    }

    @Override
    public CommentResponseDto findById(UUID commentId) {
        CommentDocument d = mongo.findOne(query(where("_id").is(commentId).and("deleted").is(false)), CommentDocument.class);
        if (d == null) {
            throw new ResourceNotFoundException("Comment", "commentId", commentId);
        }
        return CommentResponseDto.builder()
                .commentId(d.getId())
                .postId(d.getPostId())
                .parentCommentId(d.getParentId())
                .authorId(d.getAuthor() == null ? null : d.getAuthor().getUserId())
                .authorUsername(d.getAuthor() == null ? null : d.getAuthor().getUsername())
                .content(d.getContent())
                .likeCount(d.getStats().getLikes())
                .replyCount(d.getStats().getReplies())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private static CommentSummaryDto toSummary(CommentDocument d) {
        return CommentSummaryDto.builder()
                .commentId(d.getId())
                .postId(d.getPostId())
                .parentCommentId(d.getParentId())
                .authorId(d.getAuthor() == null ? null : d.getAuthor().getUserId())
                .authorUsername(d.getAuthor() == null ? null : d.getAuthor().getUsername())
                .content(d.getContent())
                .likeCount(d.getStats().getLikes())
                .replyCount(d.getStats().getReplies())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
