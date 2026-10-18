package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;

import java.util.List;

/**
 * Language-model analysis of a post (DA2 Goal 1). Implementations must return a result, never
 * throw for model-side problems (refusal, malformed output): those are recorded as a failure so
 * they count in the evaluation (Goal 4).
 */
public interface PostAnalysisPort {

    Result analyze(String content, Catalog catalog);

    /** Values the model must choose from. */
    record Catalog(List<String> speciesNames, List<String> tagCodes) {
    }

    /**
     * @param suggestion null when {@code failure} is set
     * @param rawOutput  what the model returned (JSON), kept for auditing
     */
    record Result(String model, int promptVersion, String rawOutput, PostAnalysisSuggestion suggestion,
                  String failure, long latencyMs, long inputTokens, long outputTokens) {

        public static Result failed(String model, int promptVersion, String rawOutput, String failure, long latencyMs) {
            return new Result(model, promptVersion, rawOutput, null, failure, latencyMs, 0, 0);
        }

        public boolean succeeded() {
            return failure == null && suggestion != null;
        }
    }
}
