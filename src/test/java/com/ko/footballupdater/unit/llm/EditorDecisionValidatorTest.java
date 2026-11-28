package com.ko.footballupdater.unit.llm;

import com.ko.footballupdater.llm.EditorDecisionValidator;
import com.ko.footballupdater.llm.models.EditorPostDecision;
import com.ko.footballupdater.llm.models.EditorPostType;
import com.ko.footballupdater.llm.models.EditorResponse;
import com.ko.footballupdater.llm.models.EditorResult;
import com.ko.footballupdater.llm.models.EditorSummaryGroup;
import com.ko.footballupdater.models.Post;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorDecisionValidatorTest {

    private final Post post1 = LlmTestData.post(1, "Jamie Rivers");
    private final Post post2 = LlmTestData.post(2, "Casey Moor");
    private final Post contextPost = LlmTestData.post(3, "Robin Hale");

    private Map<Integer, Post> candidates() {
        Map<Integer, Post> candidates = new LinkedHashMap<>();
        candidates.put(1, post1);
        candidates.put(2, post2);
        candidates.put(3, contextPost);
        return candidates;
    }

    private EditorResult validate(List<EditorPostDecision> decisions, List<EditorSummaryGroup> groups) {
        return EditorDecisionValidator.validate(new EditorResponse(decisions, groups), candidates(), Set.of(1, 2));
    }

    @Test
    public void validate_validDecision_kept() {
        EditorResult result = validate(List.of(
                new EditorPostDecision(1, true, EditorPostType.STANDOUT, List.of("goals", "assists"), "Scored 2 and assisted 1")), List.of());

        assertEquals(1, result.decisions().size());
        assertEquals(List.of("goals", "assists"), result.decisions().get(1).featuredStats());
        assertEquals("Scored 2 and assisted 1", result.decisions().get(1).reason());
        assertTrue(result.decisions().get(1).fromLlm());
    }

    @Test
    public void validate_unknownAndContextPostIds_dropped() {
        EditorResult result = validate(List.of(
                new EditorPostDecision(99, true, EditorPostType.ALL_STAT, List.of(), "x"),
                new EditorPostDecision(3, true, EditorPostType.ALL_STAT, List.of(), "context only"),
                new EditorPostDecision(null, true, EditorPostType.ALL_STAT, List.of(), "x")), List.of());

        assertTrue(result.decisions().isEmpty());
    }

    @Test
    public void validate_duplicatePostId_firstKept() {
        EditorResult result = validate(List.of(
                new EditorPostDecision(1, true, EditorPostType.ALL_STAT, List.of(), "first"),
                new EditorPostDecision(1, false, EditorPostType.ALL_STAT, List.of(), "second")), List.of());

        assertEquals("first", result.decisions().get(1).reason());
    }

    @Test
    public void validate_unknownOrNullStatNames_dropped() {
        // tackles is null on the post, madeUpStat doesn't exist
        EditorResult result = validate(List.of(
                new EditorPostDecision(1, true, EditorPostType.STANDOUT, List.of("goals", "tackles", "madeUpStat", "goals"), "ok")), List.of());

        assertEquals(List.of("goals"), result.decisions().get(1).featuredStats());
    }

    @Test
    public void validate_moreThanFiveFeaturedStats_decisionDropped() {
        EditorResult result = validate(List.of(
                new EditorPostDecision(1, true, EditorPostType.STANDOUT,
                        List.of("goals", "assists", "shots", "minutesPlayed", "passesAttempted", "xg"), "ok")), List.of());

        assertTrue(result.decisions().isEmpty());
    }

    @Test
    public void validate_standoutWithoutValidStats_decisionDropped() {
        EditorResult result = validate(List.of(
                new EditorPostDecision(1, true, EditorPostType.STANDOUT, List.of("tackles"), "ok")), List.of());

        assertTrue(result.decisions().isEmpty());
    }

    @Test
    public void validate_missingIncludeOrType_dropped() {
        EditorResult result = validate(List.of(
                new EditorPostDecision(1, null, EditorPostType.ALL_STAT, List.of(), "x"),
                new EditorPostDecision(2, true, null, List.of(), "x")), List.of());

        assertTrue(result.decisions().isEmpty());
    }

    @Test
    public void validate_reasonWithInventedNumber_reasonReplaced() {
        EditorResult result = validate(List.of(
                new EditorPostDecision(1, true, EditorPostType.ALL_STAT, List.of(), "Rated 9.1 by fans")), List.of());

        assertFalse(result.decisions().get(1).reason().contains("9.1"));
    }

    @Test
    public void validate_summaryGroups_invalidMembersDropped() {
        EditorResult result = validate(List.of(
                        new EditorPostDecision(2, false, EditorPostType.ALL_STAT, List.of(), "skip")),
                List.of(
                        new EditorSummaryGroup("Northfield", List.of(1, 3, 99), "Same team"),
                        new EditorSummaryGroup("Too small", List.of(1, 2), "2 is excluded"),
                        new EditorSummaryGroup("Matchday 40", List.of(1, 3), "x")));

        assertEquals(2, result.summaryGroups().size());
        assertEquals(List.of(1, 3), result.summaryGroups().get(0).postIds());
        // 40 isn't in either post's data, so the name is cleared
        assertEquals("", result.summaryGroups().get(1).name());
    }

    @Test
    public void validate_nullResponse_empty() {
        EditorResult result = EditorDecisionValidator.validate(null, candidates(), Set.of(1));
        assertTrue(result.decisions().isEmpty());
        assertTrue(result.summaryGroups().isEmpty());
    }
}
