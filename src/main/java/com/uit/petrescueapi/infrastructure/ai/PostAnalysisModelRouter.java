package com.uit.petrescueapi.infrastructure.ai;

import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Sends each call to the provider of the chosen model, so a runtime switch can also change provider. */
@Component
@Primary
@ConditionalOnProperty(name = "app.ai.post-analysis.enabled", havingValue = "true")
public class PostAnalysisModelRouter implements PostAnalysisPort {

    private final ClaudePostAnalysisAdapter claude;
    private final GeminiPostAnalysisAdapter gemini;

    public PostAnalysisModelRouter(ClaudePostAnalysisAdapter claude, GeminiPostAnalysisAdapter gemini) {
        this.claude = claude;
        this.gemini = gemini;
    }

    @Override
    public Result analyze(String content, Catalog catalog, String model) {
        return (model.startsWith("gemini-") ? gemini : claude).analyze(content, catalog, model);
    }
}
