package com.ko.footballupdater.unit.llm;

import com.ko.footballupdater.llm.LlmClient;
import com.ko.footballupdater.llm.LlmException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * LlmClient stub returning queued responses (or throwing queued exceptions) and recording prompts.
 */
public class StubLlmClient implements LlmClient {

    public record Call(String model, String systemPrompt, String userPrompt, List<LlmImage> images, Class<?> responseType) {
    }

    private boolean enabled = true;
    private final Deque<Object> responses = new ArrayDeque<>();
    public final List<Call> calls = new ArrayList<>();

    public StubLlmClient enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public StubLlmClient thenReturn(Object response) {
        responses.add(response);
        return this;
    }

    public StubLlmClient thenThrow(LlmException exception) {
        responses.add(exception);
        return this;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public <T> T generateStructured(String systemPrompt, String userPrompt, Class<T> responseType) throws LlmException {
        return generateStructured(null, systemPrompt, userPrompt, List.of(), responseType);
    }

    @Override
    public <T> T generateStructured(String model, String systemPrompt, String userPrompt, List<LlmImage> images, Class<T> responseType) throws LlmException {
        calls.add(new Call(model, systemPrompt, userPrompt, images, responseType));
        Object next = responses.poll();
        if (next == null) {
            throw new LlmException("No stubbed response");
        }
        if (next instanceof LlmException ex) {
            throw ex;
        }
        return responseType.cast(next);
    }
}
