package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.petrescueapi.application.dto.matching.MatchResultDto;
import com.uit.petrescueapi.domain.valueobject.MatchCertainty;
import com.uit.petrescueapi.domain.valueobject.MatchLevel;
import com.uit.petrescueapi.domain.valueobject.MatchResult;
import com.uit.petrescueapi.domain.valueobject.MatchWarningCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** Turns the match columns frozen on an application back into what the API shows. */
@Component
@RequiredArgsConstructor
@Slf4j
class AdoptionMatchMapper {

    private final ObjectMapper objectMapper;

    /** For lists: the outcome and the warning flags, without reasons. Null when nothing was matched. */
    MatchResultDto summary(String level, BigDecimal score, String certainty, String warnings, Boolean acknowledged) {
        if (level == null) {
            return null;
        }
        return new MatchResultDto(MatchLevel.valueOf(level), score == null ? null : score.doubleValue(),
                certainty == null ? null : MatchCertainty.valueOf(certainty), null, null, warnings(warnings), null,
                Boolean.TRUE.equals(acknowledged));
    }

    /** For the detail screen: the full frozen result. Falls back to the summary if the snapshot cannot be read. */
    MatchResultDto detail(String snapshot, String level, BigDecimal score, String certainty, String warnings,
                          Boolean acknowledged, Integer weightsVersion) {
        MatchResultDto summary = summary(level, score, certainty, warnings, acknowledged);
        if (summary == null || snapshot == null) {
            return summary;
        }
        try {
            JsonNode result = objectMapper.readTree(snapshot).get("result");
            MatchResult frozen = objectMapper.treeToValue(result, MatchResult.class);
            return MatchResultDto.forStaff(frozen).acknowledged(Boolean.TRUE.equals(acknowledged));
        } catch (Exception e) {
            log.warn("Unreadable match snapshot (weights version {}): {}", weightsVersion, e.getMessage());
            return summary;
        }
    }

    private static List<MatchResult.Warning> warnings(String codes) {
        if (codes == null || codes.isBlank()) {
            return List.of();
        }
        return Arrays.stream(codes.split(","))
                .map(String::trim)
                .map(MatchWarningCode::valueOf)
                .map(code -> new MatchResult.Warning(code, code.text()))
                .toList();
    }
}
