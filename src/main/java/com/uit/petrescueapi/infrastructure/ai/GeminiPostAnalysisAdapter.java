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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * {@link PostAnalysisPort} on the Gemini Interactions REST API, for testing the pipeline on the free
 * tier. Same prompt and schema as Claude ({@link PostAnalysisContract}), so metrics compare per model.
 *
 * <p>Google may use free tier content to improve its products: test with fake data, not user posts.
 * Model-side problems are returned as a failed result, never thrown.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.post-analysis.enabled", havingValue = "true")
public class GeminiPostAnalysisAdapter implements PostAnalysisPort {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The schema Claude gets; {@code $schema} is not a documented Gemini keyword. */
    static final JsonNode OUTPUT_SCHEMA = geminiSchema();

    private final RestClient client;
    private final String apiKey;

    @Autowired
    public GeminiPostAnalysisAdapter(@Value("${app.ai.gemini.api-key:}") String apiKey) {
        this(RestClient.builder().baseUrl(BASE_URL).requestFactory(requestFactory()).build(), apiKey);
        if (!apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is set: on the free tier Google may use post content to improve its products. Test with fake data only.");
        }
    }

    GeminiPostAnalysisAdapter(RestClient client, String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public Result analyze(String content, Catalog catalog, String model) {
        long started = System.currentTimeMillis();
        if (apiKey.isBlank()) {
            return Result.failed(model, PostAnalysisContract.PROMPT_VERSION, null, "GEMINI_API_KEY is not set", elapsed(started));
        }
        String body;
        try {
            body = client.post()
                    .uri("/interactions")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(JSON.writeValueAsString(requestBody(content, catalog, model)))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.warn("Gemini post analysis failed with HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return Result.failed(model, PostAnalysisContract.PROMPT_VERSION, null, "api error " + e.getStatusCode().value(), elapsed(started));
        } catch (Exception e) {
            log.warn("Gemini post analysis failed", e);
            return Result.failed(model, PostAnalysisContract.PROMPT_VERSION, null, "client error: " + e.getClass().getSimpleName(), elapsed(started));
        }
        return toResult(body, model, started);
    }

    // ── Pure helpers (unit-tested) ──────────────────────────

    static Map<String, Object> requestBody(String content, Catalog catalog, String model) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("system_instruction", PostAnalysisContract.systemPrompt(catalog));
        body.put("input", "<post>\n" + content + "\n</post>");
        body.put("response_format", Map.of("type", "text", "mime_type", "application/json", "schema", OUTPUT_SCHEMA));
        body.put("generation_config", Map.of("max_output_tokens", 4096));
        body.put("store", false);
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
        String usedModel = root.path("model").asText(model);
        String status = root.path("status").asText("");
        String raw = outputText(root);
        if ("incomplete".equals(status)) {
            return Result.failed(usedModel, version, raw, "output truncated (incomplete)", elapsed(started));
        }
        if (!"completed".equals(status)) {
            String message = root.path("errors").path(0).path("message").asText("");
            return Result.failed(usedModel, version, raw, "status " + status + (message.isEmpty() ? "" : ": " + message), elapsed(started));
        }
        try {
            JsonNode usage = root.path("usage");
            return new Result(usedModel, version, raw, PostAnalysisContract.toSuggestion(PostAnalysisContract.parse(raw)), null,
                    elapsed(started), usage.path("total_input_tokens").asLong(), usage.path("total_output_tokens").asLong());
        } catch (RuntimeException e) {
            return Result.failed(usedModel, version, raw, "malformed output: " + e.getMessage(), elapsed(started));
        }
    }

    private static String outputText(JsonNode root) {
        return StreamSupport.stream(root.path("steps").spliterator(), false)
                .filter(step -> "model_output".equals(step.path("type").asText()))
                .flatMap(step -> StreamSupport.stream(step.path("content").spliterator(), false))
                .filter(block -> "text".equals(block.path("type").asText()))
                .map(block -> block.path("text").asText())
                .collect(Collectors.joining());
    }

    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    private static JsonNode geminiSchema() {
        ObjectNode schema = ObjectMappers.jsonMapper().valueToTree(PostAnalysisContract.OUTPUT_FORMAT.schema());
        schema.remove("$schema");
        return schema;
    }

    private static long elapsed(long started) {
        return System.currentTimeMillis() - started;
    }
}
