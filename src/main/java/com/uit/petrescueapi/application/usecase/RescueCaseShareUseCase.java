package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.analysis.PostFacets;
import com.uit.petrescueapi.application.dto.post.CreatePostRequestDto;
import com.uit.petrescueapi.application.port.command.PostCommandPort;
import com.uit.petrescueapi.application.port.command.RescueCaseSharePort;
import com.uit.petrescueapi.application.port.out.AnalysisCatalogPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort;
import com.uit.petrescueapi.domain.entity.Post;
import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.valueobject.PostCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * "Report and tell the community" in one step. The case lives in PostgreSQL and the post in MongoDB,
 * so there is no shared transaction: the case is committed first and the post is best effort.
 *
 * <p>The post carries no images of its own. Its card shows the case's first image, read from the
 * case, so nothing goes stale when the uploaded files move out of the temporary folder.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RescueCaseShareUseCase implements RescueCaseSharePort {

    private static final int MAX_CONTENT = 2000;

    private final PostCommandPort posts;
    private final PostAnalysisStorePort analysisStore;
    private final AnalysisCatalogPort catalog;

    @Override
    public Optional<UUID> shareAsPost(RescueCase rescueCase, String content, UUID authorId) {
        try {
            Post post = posts.create(CreatePostRequestDto.builder()
                    .content(content == null || content.isBlank() ? defaultContent(rescueCase) : content.trim())
                    .rescueCaseId(rescueCase.getCaseId())
                    .build(), authorId);
            labelAsRescue(post.getPostId(), rescueCase, authorId);
            return Optional.of(post.getPostId());
        } catch (RuntimeException e) {
            log.warn("Rescue case {} was saved but its post could not be published: {}", rescueCase.getCaseId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * The reporter stated all of this on the report form, so the labels are theirs, not a guess: the
     * post is filterable as a rescue post at once instead of waiting for AI analysis and a confirmation.
     */
    private void labelAsRescue(UUID postId, RescueCase rescueCase, UUID authorId) {
        try {
            analysisStore.confirm(postId, new PostFacets(PostCategory.RESCUE, catalogSpecies(rescueCase.getSpecies()),
                    null, blankToNull(rescueCase.getProvinceName()), true), List.of(), authorId);
        } catch (RuntimeException e) {
            log.warn("Post {} was published without rescue labels: {}", postId, e.getMessage());
        }
    }

    /** The case holds free text; the feed filters on catalog names. Unknown species stay unlabelled. */
    private String catalogSpecies(String species) {
        if (species == null || species.isBlank()) {
            return null;
        }
        return catalog.speciesNames().stream().filter(name -> name.equalsIgnoreCase(species.trim())).findFirst().orElse(null);
    }

    private static String defaultContent(RescueCase rescueCase) {
        String headline = Stream.of("Cần giúp đỡ", blankToNull(rescueCase.getSpecies()))
                .filter(java.util.Objects::nonNull).collect(Collectors.joining(": "));
        String place = Stream.of(blankToNull(rescueCase.getLocationText()), blankToNull(rescueCase.getWardName()),
                        blankToNull(rescueCase.getProvinceName()))
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.joining(", "));
        String text = Stream.of(headline + (place.isEmpty() ? "" : " tại " + place) + ".", blankToNull(rescueCase.getDescription()))
                .filter(java.util.Objects::nonNull).collect(Collectors.joining(" "));
        return text.length() <= MAX_CONTENT ? text : text.substring(0, MAX_CONTENT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
