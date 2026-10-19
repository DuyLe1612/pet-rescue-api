package com.uit.petrescueapi.infrastructure.config;

import com.uit.petrescueapi.application.port.command.PostAnalysisCommandPort;
import com.uit.petrescueapi.application.port.command.RescueCaseCommandPort;
import com.uit.petrescueapi.application.port.out.AnalysisCatalogPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisStorePort;
import com.uit.petrescueapi.application.service.PostAnalysisValidator;
import com.uit.petrescueapi.infrastructure.ai.PostAnalysisWorker;
import com.uit.petrescueapi.presentation.controller.PostAnalysisController;
import com.uit.petrescueapi.presentation.mapper.RescueCaseWebMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PostAnalysisWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PostAnalysisConfig.class, PostAnalysisController.class, PostAnalysisWorker.class,
                    PostAnalysisValidator.class)
            .withBean(PostAnalysisStorePort.class, () -> mock(PostAnalysisStorePort.class))
            .withBean(AnalysisCatalogPort.class, () -> mock(AnalysisCatalogPort.class))
            .withBean(RescueCaseCommandPort.class, () -> mock(RescueCaseCommandPort.class))
            .withBean(RescueCaseWebMapper.class, () -> mock(RescueCaseWebMapper.class));

    @Test
    void absentOnPostgres() {
        runner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(PostAnalysisCommandPort.class);
            assertThat(ctx).doesNotHaveBean(PostAnalysisController.class);
            assertThat(ctx).doesNotHaveBean(PostAnalysisWorker.class);
        });
    }

    @Test
    void endpointsWithoutWorkerWhenAiIsDisabled() {
        runner.withPropertyValues("app.storage.post=mongo").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).hasSingleBean(PostAnalysisCommandPort.class);
            assertThat(ctx).hasSingleBean(PostAnalysisController.class);
            assertThat(ctx).doesNotHaveBean(PostAnalysisWorker.class);
        });
    }

    @Test
    void workerRunsWhenAiIsEnabled() {
        runner.withPropertyValues("app.storage.post=mongo", "app.ai.post-analysis.enabled=true")
                .withBean(PostAnalysisPort.class, () -> mock(PostAnalysisPort.class))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(PostAnalysisWorker.class);
                });
    }
}
