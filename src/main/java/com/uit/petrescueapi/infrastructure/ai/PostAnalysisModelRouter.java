package com.uit.petrescueapi.infrastructure.ai;

import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Sends each call to the provider of the chosen model, so a runtime switch can also change provider:
 * {@code openrouter/...} goes to OpenRouter, {@code gemini-...} to Gemini, anything else to Claude.
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.ai.post-analysis.enabled", havingValue = "true")
public class PostAnalysisModelRouter implements PostAnalysisPort {

    private final ClaudePostAnalysisAdapter claude;
    private final GeminiPostAnalysisAdapter gemini;
    private final OpenRouterPostAnalysisAdapter openRouter;

    public PostAnalysisModelRouter(ClaudePostAnalysisAdapter claude, GeminiPostAnalysisAdapter gemini,
                                   OpenRouterPostAnalysisAdapter openRouter) {
        this.claude = claude;
        this.gemini = gemini;
        this.openRouter = openRouter;
    }

    @Override
    public Result analyze(String content, Catalog catalog, String model) {
        if (model.startsWith(OpenRouterPostAnalysisAdapter.MODEL_PREFIX)) {
            return openRouter.analyze(content, catalog, model);
        }
        return (model.startsWith("gemini-") ? gemini : claude).analyze(content, catalog, model);
    }
}
