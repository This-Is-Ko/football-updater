package com.ko.footballupdater.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the LLM editor and caption agents.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /**
     * When false, the rule-based PerformanceAnalysisService and template captions are used.
     */
    private boolean enabled = false;

    /**
     * Anthropic API key. When blank the SDK's default credential lookup (ANTHROPIC_API_KEY etc.) is used.
     */
    private String apiKey;

    private String model = "claude-sonnet-5-5";

    /**
     * Maximum number of posts sent to the editor and caption agents per run.
     */
    private int maxPostsPerRun = 10;

    private long maxTokens = 16000;

    /**
     * Request timeout in seconds.
     */
    private long timeoutSeconds = 120;

    /**
     * Location of the few-shot caption examples, editable to match the account's voice.
     */
    private String captionExamplesLocation = "classpath:llm/caption-examples.txt";
}
