package com.uit.petrescueapi.infrastructure.ai;

import com.anthropic.core.ObjectMappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link PostAnalysisPort} on OpenRouter's chat completions API, which gives one key access to many
 * models. Same prompt and schema as Claude and Gemini ({@link PostAnalysisContract}), so metrics
 * compare per model.
 *
 * <p>A model is selected as {@code openrouter/<vendor>/<model>}, for example
 * {@code openrouter/google/gemini-2.5-flash}; the prefix is dropped before the call.</p>
 *
 * <p>OpenRouter forwards the text to whichever provider serves the model, so that provider's data
 * policy applies: test with fake data, not user posts. Model-side problems are returned as a failed
 * result, never thrown.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.post-analysis.enabled", havingValue = "true")
public class OpenRouterPostAnalysisAdapter implements PostAnalysisPort {

    /** Models whose name starts with this go to OpenRouter; see {@link PostAnalysisModelRouter}. */
    public static final String MODEL_PREFIX = "openrouter/";

    private static final String BASE_URL = "https://openrouter.ai/api/v1";
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(90);
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The schema Claude gets; {@code $schema} is not accepted by every provider behind OpenRouter. */
    static final JsonNode OUTPUT_SCHEMA = outputSchema();

    private final RestClient client;
    private final String apiKey;

    @Autowired
    public OpenRouterPostAnalysisAdapter(@Value("${app.ai.openrouter.api-key:}") String apiKey) {
        this(RestClient.builder().baseUrl(BASE_URL).requestFactory(requestFactory()).build(), apiKey);
        if (!apiKey.isBlank()) {
            log.warn("OpenRouter key is set: post content is forwarded to the provider of the chosen model. Test with fake data only.");
        }
    }

    OpenRouterPostAnalysisAdapter(RestClient client, String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public Result analyze(String content, Catalog catalog, String model) {
        long started = System.currentTimeMillis();
        int version = PostAnalysisContract.PROMPT_VERSION;
        if (apiKey.isBlank()) {
            return Result.failed(model, version, null, "OPENROUTER_API_KEY is not set", elapsed(started));
        }
        String body;
        try {
            body = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(JSON.writeValueAsString(requestBody(content, catalog, upstreamModel(model))))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.warn("OpenRouter post analysis failed with HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return Result.failed(model, version, null, "api error " + e.getStatusCode().value(), elapsed(started));
        } catch (Exception e) {
            log.warn("OpenRouter post analysis failed", e);
            return Result.failed(model, version, null, "client error: " + e.getClass().getSimpleName(), elapsed(started));
        }
        return toResult(body, model, started);
    }

    // ── Pure helpers (unit-tested) ──────────────────────────

    /** {@code openrouter/google/gemini-2.5-flash} → {@code google/gemini-2.5-flash}. */
    static String upstreamModel(String model) {
        return model.startsWith(MODEL_PREFIX) ? model.substring(MODEL_PREFIX.length()) : model;
    }

    static Map<String, Object> requestBody(String content, Catalog catalog, String upstreamModel) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", upstreamModel);
        body.put("messages", List.of(
                Map.of("role", "system", "content", PostAnalysisContract.systemPrompt(catalog)),
                Map.of("role", "user", "content", "<post>\n" + content + "\n</post>")));
        body.put("response_format", Map.of("type", "json_schema",
                "json_schema", Map.of("name", "post_analysis", "strict", true, "schema", OUTPUT_SCHEMA)));
        body.put("temperature", 0);
        body.put("max_tokens", 4096);
        // Only route to providers that honour response_format: a provider that ignores it would
        // return free text and be counted as a malformed answer of the model.
        body.put("provider", Map.of("require_parameters", true));
        return body;
    }

    private static Result toResult(String body, String model, long started) {
        int version = PostAnalysisContract.PROMPT_VERSION;
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (Exception e) {
            return Result.failed(model, version, body, "malformed response: " + e.getMessage(), elapsed(started));
        }
        // OpenRouter reports some upstream errors with HTTP 200 and an error object.
        if (root.hasNonNull("error")) {
            String message = root.path("error").path("message").asText("unknown");
            return Result.failed(model, version, null, "api error: " + message, elapsed(started));
        }
        JsonNode choice = root.path("choices").path(0);
        String usedModel = root.hasNonNull("model") ? MODEL_PREFIX + root.path("model").asText() : model;
        String raw = stripCodeFence(choice.path("message").path("content").asText(""));
        String finish = choice.path("finish_reason").asText("");
        if ("length".equals(finish)) {
            return Result.failed(usedModel, version, raw, "output truncated (length)", elapsed(started));
        }
        if (raw.isBlank()) {
            return Result.failed(usedModel, version, raw, "empty output" + (finish.isEmpty() ? "" : " (" + finish + ")"), elapsed(started));
        }
        try {
            JsonNode usage = root.path("usage");
            return new Result(usedModel, version, raw, PostAnalysisContract.toSuggestion(PostAnalysisContract.parse(raw)), null,
                    elapsed(started), usage.path("prompt_tokens").asLong(), usage.path("completion_tokens").asLong());
        } catch (RuntimeException e) {
            return Result.failed(usedModel, version, raw, "malformed output: " + e.getMessage(), elapsed(started));
        }
    }

    /** Some models wrap JSON in a markdown fence even when asked not to. */
    static String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFence <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, lastFence).trim();
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    private static JsonNode outputSchema() {
        ObjectNode schema = ObjectMappers.jsonMapper().valueToTree(PostAnalysisContract.OUTPUT_FORMAT.schema());
        schema.remove("$schema");
        return schema;
    }

    private static long elapsed(long started) {
        return System.currentTimeMillis() - started;
    }
}
