package com.ko.footballupdater.unit.llm;

import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.CaptionAgent;
import com.ko.footballupdater.llm.LlmException;
import com.ko.footballupdater.llm.models.CaptionResponse;
import com.ko.footballupdater.models.CaptionSource;
import com.ko.footballupdater.models.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaptionAgentTest {

    private static final String HASHTAGS = "\n\n#jamierivers #football";

    private CaptionAgent captionAgent;
    private StubLlmClient llmClient;
    private Post post;
    private final Set<String> knownNames = Set.of("Jamie Rivers", "Casey Moor", "Northfield FC", "Southport United");

    @BeforeEach
    public void setup() {
        captionAgent = new CaptionAgent();
        llmClient = new StubLlmClient();
        LlmProperties llmProperties = new LlmProperties();
        ReflectionTestUtils.setField(captionAgent, "llmClient", llmClient);
        ReflectionTestUtils.setField(captionAgent, "llmProperties", llmProperties);
        ReflectionTestUtils.setField(captionAgent, "resourceLoader", new DefaultResourceLoader());
        post = LlmTestData.post(1, "Jamie Rivers");
    }

    @Test
    public void generateCaption_validFirstAttempt_llmCaptionWithHashtags() {
        llmClient.thenReturn(new CaptionResponse("{player} with 2 goals for {team} against {opponent} 🔥"));

        CaptionAgent.CaptionResult result = captionAgent.generateCaption(post, List.of("goals"), HASHTAGS, knownNames);

        assertEquals(CaptionSource.LLM, result.source());
        assertEquals("Jamie Rivers with 2 goals for Northfield FC against Southport United 🔥" + HASHTAGS, result.caption());
        assertEquals(1, llmClient.calls.size());
        // Few-shot examples from the resource file are in the system prompt
        assertTrue(llmClient.calls.get(0).systemPrompt().contains("Swipe for the full stats"));
        assertTrue(llmClient.calls.get(0).userPrompt().contains("\"featuredStats\""));
    }

    @Test
    public void generateCaption_invalidThenValid_retriesWithErrors() {
        llmClient.thenReturn(new CaptionResponse("{player} with 5 goals"))
                .thenReturn(new CaptionResponse("{player} with 2 goals"));

        CaptionAgent.CaptionResult result = captionAgent.generateCaption(post, List.of(), HASHTAGS, knownNames);

        assertEquals(CaptionSource.LLM, result.source());
        assertEquals(2, llmClient.calls.size());
        String retryPrompt = llmClient.calls.get(1).userPrompt();
        assertTrue(retryPrompt.contains("{player} with 5 goals"));
        assertTrue(retryPrompt.contains("Numbers not found in the stats or match data: 5"));
    }

    @Test
    public void generateCaption_invalidTwice_templateCaption() {
        llmClient.thenReturn(new CaptionResponse("{player} with 5 goals"))
                .thenReturn(new CaptionResponse("{player} and Casey Moor star"));

        CaptionAgent.CaptionResult result = captionAgent.generateCaption(post, List.of(), HASHTAGS, knownNames);

        assertEquals(CaptionSource.TEMPLATE, result.source());
        assertEquals("template caption", result.caption());
        assertEquals(2, llmClient.calls.size());
    }

    @Test
    public void generateCaption_llmThrows_templateCaption() {
        llmClient.thenThrow(new LlmException("rate limited"));

        CaptionAgent.CaptionResult result = captionAgent.generateCaption(post, List.of(), HASHTAGS, knownNames);

        assertEquals(CaptionSource.TEMPLATE, result.source());
        assertEquals("template caption", result.caption());
    }

    @Test
    public void generateCaption_llmDisabled_templateCaptionWithoutCall() {
        llmClient.enabled(false);

        CaptionAgent.CaptionResult result = captionAgent.generateCaption(post, List.of(), HASHTAGS, knownNames);

        assertEquals(CaptionSource.TEMPLATE, result.source());
        assertTrue(llmClient.calls.isEmpty());
    }

    @Test
    public void parseExamples_skipsCommentsAndSplitsOnSeparator() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> examples = (List<String>) ReflectionTestUtils.invokeMethod(CaptionAgent.class, "parseExamples",
                "# comment\nFirst example\n---\nSecond\nexample\n---\n\n");

        assertEquals(List.of("First example", "Second\nexample"), examples);
    }

    @Test
    public void bundledExamples_threeExamplesLoaded() {
        @SuppressWarnings("unchecked")
        List<String> examples = (List<String>) ReflectionTestUtils.invokeMethod(captionAgent, "loadExamples");

        assertEquals(3, examples.size());
    }
}
