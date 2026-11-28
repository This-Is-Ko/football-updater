package com.ko.footballupdater.llm;

import java.util.List;

/**
 * Minimal LLM abstraction so the agents can be tested with a stub.
 */
public interface LlmClient {

    /**
     * An image sent to the model, e.g. JPEG bytes with media type "image/jpeg".
     */
    record LlmImage(byte[] data, String mediaType) {
    }

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

    /**
     * Same as {@link #generateStructured(String, String, Class)} with images before the prompt text,
     * sent to the given model (e.g. the vision model).
     */
    <T> T generateStructured(String model, String systemPrompt, String userPrompt, List<LlmImage> images, Class<T> responseType) throws LlmException;
}
