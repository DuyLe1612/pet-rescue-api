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
        assertThatCode(() -> ClaudePostAnalysisAdapter.OUTPUT_FORMAT.validate()).doesNotThrowAnyException();
        assertThat(ClaudePostAnalysisAdapter.OUTPUT_FORMAT.schema().toString())
                .contains("category").contains("needsRescue").contains("RESCUE");
    }

    @Test
    void systemPromptIsDeterministicAndListsTheCatalog() {
        String a = ClaudePostAnalysisAdapter.systemPrompt(catalog);
        String b = ClaudePostAnalysisAdapter.systemPrompt(new Catalog(List.of("Cat", "Dog"), List.of("injured", "rescue")));

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

        PostAnalysisSuggestion s = ClaudePostAnalysisAdapter.toSuggestion(ClaudePostAnalysisAdapter.parse(json));

        assertThat(s.category()).isEqualTo(PostCategory.RESCUE);
        assertThat(s.species()).isEqualTo("Dog");
        assertThat(s.district()).isEqualTo("Quận 7");
        assertThat(s.urgency()).isEqualTo(RescuePriority.HIGH);
        assertThat(s.tags()).containsExactly("injured");
        assertThat(s.categoryConfidence()).isEqualTo(0.95);
    }

    @Test
    void emptyStringsMeanUnknown() {
        var out = new ClaudePostAnalysisAdapter.ModelOutput(PostCategory.GENERAL, "", "", "", "", false, "",
                ClaudePostAnalysisAdapter.Urgency.NONE, List.of(), 0.6, 0.0, 0.0);

        PostAnalysisSuggestion s = ClaudePostAnalysisAdapter.toSuggestion(out);

        assertThat(s.species()).isNull();
        assertThat(s.district()).isNull();
        assertThat(s.urgency()).isNull();
        assertThat(s.speciesConfidence()).isNull();     // no species → no species confidence
    }
}
