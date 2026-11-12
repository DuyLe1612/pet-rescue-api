package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.post.PostResponseDto;
import com.uit.petrescueapi.application.dto.post.PostCursorResponseDto;
import com.uit.petrescueapi.application.dto.post.PostFeedFilter;
import com.uit.petrescueapi.application.dto.post.PostRescueCaseDto;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.PostReferencePort;
import com.uit.petrescueapi.application.port.out.PostReferencePort.RescueCaseCard;
import com.uit.petrescueapi.application.dto.post.PostSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.PostQueryDataPort;
import com.uit.petrescueapi.application.port.query.PostQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Query (read) use-case for Post operations.
 *
 * <p>Thin orchestrator: delegates directly to {@link PostQueryDataPort}
 * (implemented by the infrastructure query adapter). No domain service
 * involvement -- queries bypass the domain layer entirely (CQRS).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostQueryUseCase implements PostQueryPort {

    private static final Set<String> OPEN_STATUSES = Set.of("REPORTED", "IN_PROGRESS");

    private final PostQueryDataPort queryDataPort;
    private final PostReferencePort references;
    private final CloudStoragePort cloudStorage;

    @Override
    public PostResponseDto findById(UUID postId) {
        log.debug("Query: find post by id {}", postId);
        PostResponseDto post = queryDataPort.findById(postId);
        if (post.getRescueCase() != null) {
            post.setRescueCase(card(references.findRescueCaseCards(List.of(post.getRescueCase().getCaseId()))
                    .get(post.getRescueCase().getCaseId())));
        }
        return post;
    }

    @Override
    public Page<PostSummaryResponseDto> findAll(String search, PostFeedFilter filter, Pageable pageable) {
        log.debug("Query: find all posts (paginated, search={})", search);
        Page<PostSummaryResponseDto> page = queryDataPort.findAllSummaries(search, filter == null ? PostFeedFilter.none() : filter, pageable);
        attachCaseCards(page.getContent());
        return page;
    }

    @Override
    public PostCursorResponseDto findFeedByCursor(LocalDateTime cursor, UUID cursorId, int size, UUID viewerId, PostFeedFilter filter) {
        log.debug("Query: find post feed by cursor {}, size {}", cursor, size);
        PostCursorResponseDto feed = queryDataPort.findFeedByCursor(cursor, cursorId, size, viewerId, filter == null ? PostFeedFilter.none() : filter);
        attachCaseCards(feed.getItems());
        return feed;
    }

    /**
     * One PostgreSQL lookup for the whole page. The case is read fresh, so a card never shows a stale
     * status, and a post whose case was deleted simply loses its card.
     */
    private void attachCaseCards(List<PostSummaryResponseDto> posts) {
        List<UUID> caseIds = posts.stream()
                .map(PostSummaryResponseDto::getRescueCase)
                .filter(Objects::nonNull)
                .map(PostRescueCaseDto::getCaseId)
                .distinct()
                .toList();
        if (caseIds.isEmpty()) {
            return;
        }
        Map<UUID, RescueCaseCard> cards = references.findRescueCaseCards(caseIds);
        posts.stream()
                .filter(post -> post.getRescueCase() != null)
                .forEach(post -> post.setRescueCase(card(cards.get(post.getRescueCase().getCaseId()))));
    }

    private PostRescueCaseDto card(RescueCaseCard card) {
        if (card == null) {
            return null;
        }
        return PostRescueCaseDto.builder()
                .caseId(card.caseId())
                .caseCode(card.caseCode())
                .status(card.status())
                .open(OPEN_STATUSES.contains(card.status()))
                .priority(card.priority())
                .species(card.species())
                .locationText(card.locationText())
                .provinceName(card.provinceName())
                .imageUrl(card.firstImagePublicId() == null ? null : cloudStorage.buildUrl(card.firstImagePublicId()))
                .build();
    }
}
