package com.ko.footballupdater.unit.llm;

import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.EditorAgent;
import com.ko.footballupdater.llm.LlmException;
import com.ko.footballupdater.llm.models.EditorPostDecision;
import com.ko.footballupdater.llm.models.EditorPostType;
import com.ko.footballupdater.llm.models.EditorResponse;
import com.ko.footballupdater.llm.models.EditorResult;
import com.ko.footballupdater.llm.models.EditorSummaryGroup;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.services.PerformanceAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorAgentTest {

    private EditorAgent editorAgent;
    private StubLlmClient llmClient;
    private LlmProperties llmProperties;

    private Post post1;
    private Post post2;

    @BeforeEach
    public void setup() {
        editorAgent = new EditorAgent();
        llmClient = new StubLlmClient();
        llmProperties = new LlmProperties();
        llmProperties.setMaxPostsPerRun(10);
        ReflectionTestUtils.setField(editorAgent, "llmClient", llmClient);
        ReflectionTestUtils.setField(editorAgent, "llmProperties", llmProperties);
        ReflectionTestUtils.setField(editorAgent, "performanceAnalysisService", new PerformanceAnalysisService());

        post1 = LlmTestData.post(1, "Jamie Rivers");
        post2 = LlmTestData.post(2, "Casey Moor");
        // No highlights for post2
        post2.getPlayerMatchPerformanceStats().setGoals(0);
        post2.getPlayerMatchPerformanceStats().setAssists(0);
        post2.getPlayerMatchPerformanceStats().setPassesAttempted(10);
    }

    @Test
    public void decide_llmDisabled_usesRuleBasedFallback() {
        llmClient.enabled(false);

        EditorResult result = editorAgent.decide(List.of(post1, post2), List.of());

        assertTrue(llmClient.calls.isEmpty());
        assertEquals(EditorPostType.STANDOUT, result.decisions().get(1).postType());
        assertEquals(List.of("goals", "assists", "passesSuccessPercentage", "passesAttempted"), result.decisions().get(1).featuredStats());
        assertEquals(EditorPostType.ALL_STAT, result.decisions().get(2).postType());
        assertTrue(result.decisions().values().stream().allMatch(d -> d.include() && !d.fromLlm()));
    }

    @Test
    public void decide_llmThrows_usesRuleBasedFallback() {
        llmClient.thenThrow(new LlmException("timeout"));

        EditorResult result = editorAgent.decide(List.of(post1, post2), List.of());

        assertEquals(1, llmClient.calls.size());
        assertEquals(2, result.decisions().size());
        assertFalse(result.decisions().get(1).fromLlm());
    }

    @Test
    public void decide_validLlmResponse_usesLlmDecisionsAndSendsContext() {
        Post contextPost = LlmTestData.post(3, "Robin Hale");
        llmClient.thenReturn(new EditorResponse(List.of(
                new EditorPostDecision(1, true, EditorPostType.STANDOUT, List.of("goals"), "Two goals"),
                new EditorPostDecision(2, false, EditorPostType.ALL_STAT, List.of(), "Quiet game")),
                List.of(new EditorSummaryGroup("Northfield", List.of(2, 3), "Same team"))));

        EditorResult result = editorAgent.decide(List.of(post1, post2), List.of(contextPost));

        assertTrue(result.decisions().get(1).fromLlm());
        assertEquals(List.of("goals"), result.decisions().get(1).featuredStats());
        assertFalse(result.decisions().get(2).include());
        // Excluded post 2 is dropped from the group, leaving fewer than 2 members
        assertTrue(result.summaryGroups().isEmpty());

        String prompt = llmClient.calls.get(0).userPrompt();
        assertTrue(prompt.contains("\"postId\" : 3"));
        assertTrue(prompt.contains("\"isNew\" : false"));
        assertTrue(prompt.contains("\"goals\" : 2"));
        assertEquals(EditorResponse.class, llmClient.calls.get(0).responseType());
    }

    @Test
    public void decide_invalidDecisionForOnePost_fallbackForThatPostOnly() {
        llmClient.thenReturn(new EditorResponse(List.of(
                new EditorPostDecision(1, true, EditorPostType.STANDOUT, List.of("notAStat"), "x"),
                new EditorPostDecision(2, true, EditorPostType.ALL_STAT, List.of(), "Solid")), List.of()));

        EditorResult result = editorAgent.decide(List.of(post1, post2), List.of());

        assertFalse(result.decisions().get(1).fromLlm());
        assertEquals(EditorPostType.STANDOUT, result.decisions().get(1).postType());
        assertTrue(result.decisions().get(2).fromLlm());
    }

    @Test
    public void decide_overPerRunCap_extraPostsUseFallback() {
        llmProperties.setMaxPostsPerRun(1);
        llmClient.thenReturn(new EditorResponse(List.of(
                new EditorPostDecision(1, true, EditorPostType.ALL_STAT, List.of(), "ok"),
                new EditorPostDecision(2, true, EditorPostType.ALL_STAT, List.of(), "not sent")), List.of()));

        EditorResult result = editorAgent.decide(List.of(post1, post2), List.of());

        assertFalse(llmClient.calls.get(0).userPrompt().contains("Casey Moor"));
        assertTrue(result.decisions().get(1).fromLlm());
        assertFalse(result.decisions().get(2).fromLlm());
    }
}
