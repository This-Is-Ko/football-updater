package com.ko.footballupdater.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredContentBlock;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredTextBlock;
import com.ko.footballupdater.configuration.LlmProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * LlmClient backed by the Anthropic Messages API, using structured outputs so responses match the given schema.
 */
@Slf4j
@Component
public class AnthropicLlmClient implements LlmClient {

    @Autowired
    private LlmProperties llmProperties;

    private volatile AnthropicClient client;

    public AnthropicLlmClient() {
    }

    // For tests
    public AnthropicLlmClient(LlmProperties llmProperties, AnthropicClient client) {
        this.llmProperties = llmProperties;
        this.client = client;
    }

    @Override
    public boolean isEnabled() {
        return llmProperties.isEnabled();
    }

    private AnthropicClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder()
                            .fromEnv()
                            .timeout(Duration.ofSeconds(llmProperties.getTimeoutSeconds()));
                    if (llmProperties.getApiKey() != null && !llmProperties.getApiKey().isBlank()) {
                        builder.apiKey(llmProperties.getApiKey());
                    }
                    client = builder.build();
                }
            }
        }
        return client;
    }

    @Override
    public <T> T generateStructured(String systemPrompt, String userPrompt, Class<T> responseType) throws LlmException {
        if (!isEnabled()) {
            throw new LlmException("LLM is disabled");
        }
        StructuredMessageCreateParams<T> params = MessageCreateParams.builder()
                .model(llmProperties.getModel())
                .maxTokens(llmProperties.getMaxTokens())
                .system(systemPrompt)
                .addUserMessage(userPrompt)
                .outputConfig(responseType)
                .build();

        StructuredMessage<T> response;
        try {
            response = getClient().messages().create(params);
        } catch (AnthropicException ex) {
            throw new LlmException("LLM request failed: " + ex.getMessage(), ex);
        }

        Optional<StopReason> stopReason = response.stopReason();
        if (stopReason.isPresent() && StopReason.REFUSAL.equals(stopReason.get())) {
            throw new LlmException("LLM refused the request");
        }
        if (stopReason.isPresent() && StopReason.MAX_TOKENS.equals(stopReason.get())) {
            throw new LlmException("LLM response was truncated (max_tokens)");
        }
        log.atInfo().setMessage("LLM request completed")
                .addKeyValue("model", llmProperties.getModel())
                .addKeyValue("inputTokens", response.usage().inputTokens())
                .addKeyValue("outputTokens", response.usage().outputTokens())
                .log();

        try {
            return response.content().stream()
                    .map(StructuredContentBlock::text)
                    .flatMap(Optional::stream)
                    .map(StructuredTextBlock::text)
                    .findFirst()
                    .orElseThrow(() -> new LlmException("LLM response has no text content"));
        } catch (LlmException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new LlmException("Unable to parse LLM response", ex);
        }
    }
}
