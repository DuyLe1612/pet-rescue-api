package com.uit.petrescueapi.infrastructure.ai;

import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort.Catalog;
import com.uit.petrescueapi.domain.valueobject.PostCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Against a mocked HTTP server — no network, no API usage. */
class GeminiPostAnalysisAdapterTest {

    static final String MODEL = "gemini-3.8-flash";
    static final String OUTPUT = """
            {"category":"RESCUE","species":"Dog","locationText":"Q7","district":"Quận 7","province":"",
             "needsRescue":true,"rescueSummary":"Leg injury","urgency":"HIGH","tags":["injured"],
             "categoryConfidence":0.9,"speciesConfidence":0.9,"locationConfidence":0.8}""";

    final Catalog catalog = new Catalog(List.of("Cat", "Dog"), List.of("injured"));

    MockRestServiceServer server;
    GeminiPostAnalysisAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://gemini.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new GeminiPostAnalysisAdapter(builder.build(), "test-key");
    }

    private static String completed(String text) {
        return """
                {"id":"v1_1","model":"gemini-3.8-flash","status":"completed",
                 "steps":[{"type":"thought"},{"type":"model_output","content":[{"type":"text","text":%s}]}],
                 "usage":{"total_input_tokens":700,"total_output_tokens":80}}""".formatted(quote(text));
    }

    private static String quote(String text) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(text);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void sendsThePromptSchemaAndKeyAndParsesTheAnswer() {
        server.expect(requestTo("https://gemini.test/interactions"))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.store").value(false))
                .andExpect(jsonPath("$.input").value("<post>\nbé cún\n</post>"))
                .andExpect(jsonPath("$.system_instruction").value(org.hamcrest.Matchers.containsString("Cat, Dog")))
                .andExpect(jsonPath("$.response_format.mime_type").value("application/json"))
                .andExpect(jsonPath("$.response_format.schema.properties.category").exists())
                .andExpect(jsonPath("$.response_format.schema.$schema").doesNotExist())
                .andRespond(withSuccess(completed(OUTPUT), MediaType.APPLICATION_JSON));

        PostAnalysisPort.Result r = adapter.analyze("bé cún", catalog, MODEL);

        server.verify();
        assertThat(r.succeeded()).isTrue();
        assertThat(r.model()).isEqualTo(MODEL);
        assertThat(r.suggestion().category()).isEqualTo(PostCategory.RESCUE);
        assertThat(r.suggestion().district()).isEqualTo("Quận 7");
        assertThat(r.inputTokens()).isEqualTo(700);
        assertThat(r.outputTokens()).isEqualTo(80);
    }

    @Test
    void rateLimitIsAFailedResultNotAnException() {
        server.expect(requestTo("https://gemini.test/interactions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":{\"message\":\"quota\"}}"));

        PostAnalysisPort.Result r = adapter.analyze("x", catalog, MODEL);

        assertThat(r.succeeded()).isFalse();
        assertThat(r.failure()).isEqualTo("api error 429");
    }

    @Test
    void failedInteractionReportsItsError() {
        server.expect(requestTo("https://gemini.test/interactions"))
                .andRespond(withSuccess("""
                        {"status":"failed","steps":[],"errors":[{"code":"x","message":"blocked"}]}""", MediaType.APPLICATION_JSON));

        assertThat(adapter.analyze("x", catalog, MODEL).failure()).isEqualTo("status failed: blocked");
    }

    @Test
    void truncatedOutputIsAFailure() {
        server.expect(requestTo("https://gemini.test/interactions"))
                .andRespond(withSuccess("""
                        {"status":"incomplete","steps":[{"type":"model_output","content":[{"type":"text","text":"{\\"cat"}]}]}""",
                        MediaType.APPLICATION_JSON));

        PostAnalysisPort.Result r = adapter.analyze("x", catalog, MODEL);

        assertThat(r.failure()).isEqualTo("output truncated (incomplete)");
        assertThat(r.rawOutput()).isEqualTo("{\"cat");
    }

    @Test
    void outputThatIsNotTheSchemaIsAFailure() {
        server.expect(requestTo("https://gemini.test/interactions"))
                .andRespond(withSuccess(completed("not json"), MediaType.APPLICATION_JSON));

        assertThat(adapter.analyze("x", catalog, MODEL).failure()).startsWith("malformed output");
    }

    @Test
    void missingKeyFailsWithoutCallingTheApi() {
        GeminiPostAnalysisAdapter noKey = new GeminiPostAnalysisAdapter(RestClient.create(), "");

        assertThat(noKey.analyze("x", catalog, MODEL).failure()).isEqualTo("GEMINI_API_KEY is not set");
    }

    // ── routing ─────────────────────────────────────────────

    @Test
    void routerPicksTheProviderFromTheModelName() {
        ClaudePostAnalysisAdapter claude = mock(ClaudePostAnalysisAdapter.class);
        GeminiPostAnalysisAdapter gemini = mock(GeminiPostAnalysisAdapter.class);
        PostAnalysisModelRouter router = new PostAnalysisModelRouter(claude, gemini);

        router.analyze("x", catalog, MODEL);

        verify(gemini).analyze(any(), any(), any());
        verifyNoInteractions(claude);

        router.analyze("x", catalog, "claude-haiku-5-5");

        verify(claude).analyze(any(), any(), any());
    }

    @Test
    void springBuildsTheProvidersWithoutAnyApiKeyAndPrefersTheRouter() {
        new ApplicationContextRunner()
                .withUserConfiguration(ClaudePostAnalysisAdapter.class, GeminiPostAnalysisAdapter.class, PostAnalysisModelRouter.class)
                .withPropertyValues("app.ai.post-analysis.enabled=true")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(PostAnalysisPort.class)).isInstanceOf(PostAnalysisModelRouter.class);
                });
    }

    @Test
    void noProviderExistsWhenAiIsDisabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(ClaudePostAnalysisAdapter.class, GeminiPostAnalysisAdapter.class, PostAnalysisModelRouter.class)
                .run(ctx -> assertThat(ctx).doesNotHaveBean(PostAnalysisPort.class));
    }
}
