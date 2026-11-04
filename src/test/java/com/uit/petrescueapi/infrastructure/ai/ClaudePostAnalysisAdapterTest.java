package com.uit.petrescueapi.infrastructure.ai;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort.Catalog;
import com.uit.petrescueapi.domain.valueobject.PostCategory;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Pure parts of the Claude adapter — no network, no API cost. */
class ClaudePostAnalysisAdapterTest {

    final Catalog catalog = new Catalog(List.of("Cat", "Dog"), List.of("injured", "rescue"));

    @Test
    void outputSchemaPassesTheSdkStructuredOutputValidation() {
        // class initialisation runs the SDK's local schema validation for ModelOutput
        assertThatCode(() -> PostAnalysisContract.OUTPUT_FORMAT.validate()).doesNotThrowAnyException();
        assertThat(PostAnalysisContract.OUTPUT_FORMAT.schema().toString())
                .contains("category").contains("needsRescue").contains("RESCUE");
    }

    @Test
    void requestUsesTheChosenModel() {
        assertThat(ClaudePostAnalysisAdapter.requestParams("x", catalog, "claude-sonnet-5-5").model().asString())
                .isEqualTo("claude-sonnet-5-5");
    }

    @Test
    void serverFallbackIsSentOnlyForModelsThatHaveIt() {
        assertThat(ClaudePostAnalysisAdapter.requestParams("x", catalog, "claude-opus-5-5")._additionalBodyProperties())
                .containsKey("fallbacks");
        assertThat(ClaudePostAnalysisAdapter.requestParams("x", catalog, "claude-haiku-5-5")._additionalBodyProperties())
                .doesNotContainKey("fallbacks");
    }

    @Test
    void systemPromptIsDeterministicAndListsTheCatalog() {
        String a = PostAnalysisContract.systemPrompt(catalog);
        String b = PostAnalysisContract.systemPrompt(new Catalog(List.of("Cat", "Dog"), List.of("injured", "rescue")));

        assertThat(a).isEqualTo(b);                     // byte-stable → cacheable prefix
        assertThat(a).contains("Cat, Dog").contains("injured, rescue");
        assertThat(a).contains("RESCUE").contains("Quận 7");
    }

    @Test
    void parsesModelJsonIntoSuggestion() {
        String json = """
                {"category":"RESCUE","species":"Dog","locationText":"Q7","district":"Quận 7","province":"",
                 "needsRescue":true,"rescueSummary":"Possible leg injury","urgency":"HIGH",
                 "tags":["injured"],"categoryConfidence":0.95,"speciesConfidence":0.9,"locationConfidence":0.8}
                """;

        PostAnalysisSuggestion s = PostAnalysisContract.toSuggestion(PostAnalysisContract.parse(json));

        assertThat(s.category()).isEqualTo(PostCategory.RESCUE);
        assertThat(s.species()).isEqualTo("Dog");
        assertThat(s.district()).isEqualTo("Quận 7");
        assertThat(s.urgency()).isEqualTo(RescuePriority.HIGH);
        assertThat(s.tags()).containsExactly("injured");
        assertThat(s.categoryConfidence()).isEqualTo(0.95);
    }

    @Test
    void emptyStringsMeanUnknown() {
        var out = new PostAnalysisContract.ModelOutput(PostCategory.GENERAL, "", "", "", "", false, "",
                PostAnalysisContract.Urgency.NONE, List.of(), 0.6, 0.0, 0.0);

        PostAnalysisSuggestion s = PostAnalysisContract.toSuggestion(out);

        assertThat(s.species()).isNull();
        assertThat(s.district()).isNull();
        assertThat(s.urgency()).isNull();
        assertThat(s.speciesConfidence()).isNull();     // no species → no species confidence
    }
}
