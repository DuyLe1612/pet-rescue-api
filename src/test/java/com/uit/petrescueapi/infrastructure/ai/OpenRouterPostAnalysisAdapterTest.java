package com.uit.petrescueapi.infrastructure.ai;

import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort.Catalog;
import com.uit.petrescueapi.domain.valueobject.PostCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Against a mocked HTTP server: no network, no API usage. */
class OpenRouterPostAnalysisAdapterTest {

    static final String MODEL = "openrouter/google/gemini-2.5-flash";
    static final String URL = "https://openrouter.test/chat/completions";
    static final String OUTPUT = """
            {"category":"RESCUE","species":"Dog","locationText":"Q7","district":"Quận 7","province":"",
             "needsRescue":true,"rescueSummary":"Leg injury","urgency":"HIGH","tags":["injured"],
             "categoryConfidence":0.9,"speciesConfidence":0.9,"locationConfidence":0.8}""";

    final Catalog catalog = new Catalog(List.of("Cat", "Dog"), List.of("injured"));

    MockRestServiceServer server;
    OpenRouterPostAnalysisAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://openrouter.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new OpenRouterPostAnalysisAdapter(builder.build(), "test-key");
    }

    private static String quote(String text) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(text);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String completion(String text, String finishReason) {
        return """
                {"id":"gen-1","model":"google/gemini-2.5-flash",
                 "choices":[{"finish_reason":"%s","message":{"role":"assistant","content":%s}}],
                 "usage":{"prompt_tokens":700,"completion_tokens":80}}""".formatted(finishReason, quote(text));
    }

    @Test
    void sendsThePromptSchemaAndKeyAndParsesTheAnswer() {
        server.expect(requestTo(URL))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // the prefix is ours; OpenRouter gets the vendor/model name
                .andExpect(jsonPath("$.model").value("google/gemini-2.5-flash"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value(org.hamcrest.Matchers.containsString("Cat, Dog")))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("<post>\nbé cún\n</post>"))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.category").exists())
                .andExpect(jsonPath("$.response_format.json_schema.schema.$schema").doesNotExist())
                .andExpect(jsonPath("$.provider.require_parameters").value(true))
                .andExpect(jsonPath("$.temperature").value(0))
                .andRespond(withSuccess(completion(OUTPUT, "stop"), MediaType.APPLICATION_JSON));

        PostAnalysisPort.Result r = adapter.analyze("bé cún", catalog, MODEL);

        server.verify();
        assertThat(r.succeeded()).isTrue();
        // metrics group by the model we were asked for, with its provider prefix
        assertThat(r.model()).isEqualTo(MODEL);
        assertThat(r.suggestion().category()).isEqualTo(PostCategory.RESCUE);
        assertThat(r.suggestion().district()).isEqualTo("Quận 7");
        assertThat(r.inputTokens()).isEqualTo(700);
        assertThat(r.outputTokens()).isEqualTo(80);
    }

    @Test
    void aModelThatWrapsItsJsonInAFenceStillCounts() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(completion("```json\n" + OUTPUT + "\n```", "stop"), MediaType.APPLICATION_JSON));

        PostAnalysisPort.Result r = adapter.analyze("x", catalog, MODEL);

        assertThat(r.succeeded()).isTrue();
        assertThat(r.rawOutput()).isEqualTo(OUTPUT.trim());
    }

    @Test
    void stripsOnlyAWholeFence() {
        assertThat(OpenRouterPostAnalysisAdapter.stripCodeFence("{\"a\":1}")).isEqualTo("{\"a\":1}");
        assertThat(OpenRouterPostAnalysisAdapter.stripCodeFence("```\n{\"a\":1}\n```")).isEqualTo("{\"a\":1}");
        assertThat(OpenRouterPostAnalysisAdapter.stripCodeFence("```json {\"a\":1}")).isEqualTo("```json {\"a\":1}");
    }

    @Test
    void upstreamModelDropsOnlyOurPrefix() {
        assertThat(OpenRouterPostAnalysisAdapter.upstreamModel("openrouter/anthropic/claude-haiku"))
                .isEqualTo("anthropic/claude-haiku");
        assertThat(OpenRouterPostAnalysisAdapter.upstreamModel("google/gemini")).isEqualTo("google/gemini");
    }

    @Test
    void rateLimitIsAFailedResultNotAnException() {
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\":{\"message\":\"rate limited\"}}"));

        PostAnalysisPort.Result r = adapter.analyze("x", catalog, MODEL);

        assertThat(r.succeeded()).isFalse();
        assertThat(r.failure()).isEqualTo("api error 429");
    }

    @Test
    void noProviderSupportingTheSchemaIsAFailedResult() {
        server.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"error\":{\"message\":\"No endpoints found\"}}"));

        assertThat(adapter.analyze("x", catalog, MODEL).failure()).isEqualTo("api error 404");
    }

    @Test
    void anErrorObjectInsideAnOkResponseIsAFailure() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"error\":{\"code\":502,\"message\":\"upstream down\"}}", MediaType.APPLICATION_JSON));

        assertThat(adapter.analyze("x", catalog, MODEL).failure()).isEqualTo("api error: upstream down");
    }

    @Test
    void truncatedOutputIsAFailure() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(completion("{\"cat", "length"), MediaType.APPLICATION_JSON));

        PostAnalysisPort.Result r = adapter.analyze("x", catalog, MODEL);

        assertThat(r.failure()).isEqualTo("output truncated (length)");
        assertThat(r.rawOutput()).isEqualTo("{\"cat");
    }

    @Test
    void emptyOutputIsAFailure() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(completion("", "content_filter"), MediaType.APPLICATION_JSON));

        assertThat(adapter.analyze("x", catalog, MODEL).failure()).isEqualTo("empty output (content_filter)");
    }

    @Test
    void outputThatIsNotTheSchemaIsAFailure() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess(completion("not json", "stop"), MediaType.APPLICATION_JSON));

        assertThat(adapter.analyze("x", catalog, MODEL).failure()).startsWith("malformed output");
    }

    @Test
    void missingKeyFailsWithoutCallingTheApi() {
        OpenRouterPostAnalysisAdapter noKey = new OpenRouterPostAnalysisAdapter(RestClient.create(), "");

        assertThat(noKey.analyze("x", catalog, MODEL).failure()).isEqualTo("OPENROUTER_API_KEY is not set");
    }
}
