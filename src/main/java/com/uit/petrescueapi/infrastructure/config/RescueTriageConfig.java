package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.application.port.command.PostAnalysisCommandPort;
import com.uit.petrescueapi.application.port.out.AnalysisCatalogPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.service.PostAnalysisValidator;
import com.uit.petrescueapi.application.usecase.RescueTriageUseCase;
import com.uit.petrescueapi.domain.repository.RescueCaseTriageRepository;
import com.uit.petrescueapi.domain.service.RescueCaseDomainService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * The model adapter is optional, exactly as for post analysis: without
 * {@code app.ai.post-analysis.enabled=true} the use case does nothing and reported cases are
 * handled as they always were.
 */
@Configuration
public class RescueTriageConfig {

    @Bean
    public RescueTriageUseCase rescueTriageUseCase(RescueCaseTriageRepository triage,
                                                   ObjectProvider<PostAnalysisPort> ai,
                                                   AnalysisCatalogPort catalog,
                                                   PostAnalysisValidator validator,
                                                   PostAnalysisCommandPort models,
                                                   RescueCaseDomainService cases) {
        return new RescueTriageUseCase(triage, Optional.ofNullable(ai.getIfAvailable()), catalog, validator, models, cases);
    }
}
