package com.uit.petrescueapi.infrastructure.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.function.SingletonSupplier;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link PostAnalysisPort} on the Claude API (DA2 Goal 1). One request per post, constrained to
 * the {@link ModelOutput} JSON schema with structured outputs — the model never answers in free text.
 *
 * <p>Only active with {@code app.ai.post-analysis.enabled=true}; the API key comes from
 * {@code ANTHROPIC_API_KEY}. The model is chosen per call (see {@code PostAnalysisUseCase}).
 * Classification is a short task, so effort is {@code low}. Server-side refusal fallback
 * ({@code fallbacks: "default"}) is enabled on the models that have it; a refusal that still happens
 * is returned as a failed result, never thrown.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.post-analysis.enabled", havingValue = "true")
public class ClaudePostAnalysisAdapter implements PostAnalysisPort {

    /** Claude Haiku 5.5 has no server-side fallback: {@code fallbacks} must not be sent for it. */
    private static final Set<String> SERVER_FALLBACK_MODELS =
            Set.of("claude-fable-5-1", "claude-opus-5-5", "claude-opus-5", "claude-sonnet-5-5");

    /** Created on first use: a Gemini-only setup has no Anthropic credentials. */
    private final SingletonSupplier<AnthropicClient> client = SingletonSupplier.of(AnthropicOkHttpClient::fromEnv);

    // ── PostAnalysisPort ────────────────────────────────────

    @Override
    public Result analyze(String content, Catalog catalog, String model) {
        long started = System.currentTimeMillis();
        MessageCreateParams params = requestParams(content, catalog, model);

        Message message;
        try {
            message = client.obtain().messages().create(params);
        } catch (AnthropicServiceException e) {
            log.warn("Claude post analysis failed with HTTP {}", e.statusCode(), e);
            return Result.failed(model, PostAnalysisContract.PROMPT_VERSION, null, "api error " + e.statusCode(), elapsed(started));
        } catch (RuntimeException e) {
            log.warn("Claude post analysis failed", e);
            return Result.failed(model, PostAnalysisContract.PROMPT_VERSION, null, "client error: " + e.getClass().getSimpleName(), elapsed(started));
        }

        String usedModel = message.model().asString();
        if (message.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            String category = message.stopDetails().flatMap(d -> d.category()).map(Object::toString).orElse("unknown");
            return Result.failed(usedModel, PostAnalysisContract.PROMPT_VERSION, null, "refusal: " + category, elapsed(started));
        }
        String raw = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .collect(Collectors.joining());
        if (message.stopReason().map(StopReason.MAX_TOKENS::equals).orElse(false)) {
            return Result.failed(usedModel, PostAnalysisContract.PROMPT_VERSION, raw, "output truncated (max_tokens)", elapsed(started));
        }
        try {
            return new Result(usedModel, PostAnalysisContract.PROMPT_VERSION, raw, PostAnalysisContract.toSuggestion(PostAnalysisContract.parse(raw)), null, elapsed(started),
                    message.usage().inputTokens(), message.usage().outputTokens());
        } catch (RuntimeException e) {
            return Result.failed(usedModel, PostAnalysisContract.PROMPT_VERSION, raw, "malformed output: " + e.getMessage(), elapsed(started));
        }
    }

    // ── Pure helpers (unit-tested) ──────────────────────────

    static MessageCreateParams requestParams(String content, Catalog catalog, String model) {
        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(4096L)
                .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                        .text(PostAnalysisContract.systemPrompt(catalog))
                        .cacheControl(CacheControlEphemeral.builder().build())
                        .build()))
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.LOW)
                        .format(PostAnalysisContract.OUTPUT_FORMAT)
                        .build())
                .addUserMessage("<post>\n" + content + "\n</post>");
        if (SERVER_FALLBACK_MODELS.contains(model)) {
            // server-side refusal fallback, routed by refusal category
            params.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                    .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }
        return params.build();
    }

    private static long elapsed(long started) {
        return System.currentTimeMillis() - started;
    }
}
