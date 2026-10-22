package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.application.port.command.RescueCaseCommandPort;
import com.uit.petrescueapi.application.port.out.AnalysisCatalogPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort;
import com.uit.petrescueapi.application.service.PostAnalysisValidator;
import com.uit.petrescueapi.application.usecase.PostAnalysisUseCase;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                                                   RescueCaseCommandPort rescueCases) {
        return new PostAnalysisUseCase(store, Optional.ofNullable(ai.getIfAvailable()), catalog, validator, rescueCases);
    }
}
