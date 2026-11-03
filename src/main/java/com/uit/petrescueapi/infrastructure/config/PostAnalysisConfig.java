package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.application.port.out.AnalysisCatalogPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort;
import com.uit.petrescueapi.application.service.PostAnalysisValidator;
import com.uit.petrescueapi.application.usecase.PostAnalysisUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

/**
 * DA2 Intelligent Post Processing on the MongoDB post documents (analysis, facets, runs).
 * The model adapter is optional: without {@code app.ai.post-analysis.enabled=true} posts simply stay PENDING.
 */
@Configuration
public class PostAnalysisConfig {

    @Bean
    public PostAnalysisUseCase postAnalysisUseCase(PostAnalysisStorePort store,
                                                   ObjectProvider<PostAnalysisPort> ai,
                                                   AnalysisCatalogPort catalog,
                                                   PostAnalysisValidator validator,
                                                   @Value("${app.ai.post-analysis.model:claude-opus-5-5}") String defaultModel,
                                                   @Value("${app.ai.post-analysis.allowed-models:claude-haiku-5-5,claude-sonnet-5-5,claude-opus-5-5,gemini-3.8-flash,gemini-3.5-flash-lite}") String[] allowedModels) {
        return new PostAnalysisUseCase(store, Optional.ofNullable(ai.getIfAvailable()), catalog, validator,
                defaultModel, List.of(allowedModels));
    }
}
