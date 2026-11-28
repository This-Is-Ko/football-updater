package com.ko.footballupdater.llm;

/**
 * Minimal LLM abstraction so the agents can be tested with a stub.
 */
public interface LlmClient {

    /**
     * @return true if the LLM is enabled and configured
     */
    boolean isEnabled();

    /**
     * Send a prompt and get back a response matching the JSON schema derived from responseType.
     *
     * @throws LlmException if the request fails, is refused or doesn't return a parseable response
     */
    <T> T generateStructured(String systemPrompt, String userPrompt, Class<T> responseType) throws LlmException;
}
