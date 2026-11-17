package com.uit.petrescueapi.domain.valueobject;

import java.util.List;

/**
 * What matching says about one adopter and one pet. {@code score} is null when a warning fired.
 * {@code weightsVersion} is stored with every application so old results stay comparable.
 */
public record MatchResult(MatchLevel level, Double score, MatchCertainty certainty, int weightsVersion,
                          List<Reason> reasons, List<Warning> warnings, List<String> preparation) {

    public record Reason(MatchCriterion code, int weight, MatchReasonStatus status, MatchCertainty certainty, String text) {
    }

    public record Warning(MatchWarningCode code, String text) {
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
}
