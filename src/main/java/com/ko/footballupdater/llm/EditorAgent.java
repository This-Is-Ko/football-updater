package com.ko.footballupdater.llm;

import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.models.EditorPostType;
import com.ko.footballupdater.llm.models.EditorResponse;
import com.ko.footballupdater.llm.models.EditorResult;
import com.ko.footballupdater.llm.models.ValidatedEditorDecision;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.services.PerformanceAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides which new posts are worth publishing, their type and the stats to feature.
 * Falls back to the rule-based PerformanceAnalysisService when the LLM is disabled, fails, or returns invalid decisions.
 */
@Slf4j
@Service
public class EditorAgent {

    static final String SYSTEM_PROMPT = """
            You are the editor of an Instagram account that posts match stat cards for a fixed list of football players.
            After each round of matches you get the new candidate posts (isNew=true) and recent unposted ones (isNew=false) as JSON.

            For every candidate with isNew=true, return one decision:
            - include: false only when the post isn't worth publishing, e.g. very few minutes or nothing notable.
            - postType: STANDOUT for a performance worth highlighting on its own image (goals, assists, a high volume of accurate passing, \
            many defensive actions, big goalkeeper numbers), ALL_STAT for a solid standard post, SUMMARY_MEMBER when it is better \
            combined with others into a matchday summary than posted alone.
            - featuredStats: up to 5 stat names copied exactly from that post's "stats" keys, most impressive first. Required for STANDOUT.
            - reason: one or two plain sentences for the human reviewer.

            Optionally suggest summaryGroups: sets of at least 2 postIds (new or recent) that would work together as one summary post, \
            for example players from the same team or matchday.

            Rules:
            - Only use postIds from the candidates.
            - Never state a number that isn't in the candidate's stats or match data. Don't calculate new numbers.
            - Don't mention players or teams that aren't in the candidates.
            """;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private PerformanceAnalysisService performanceAnalysisService;

    /**
     * @param newPosts     posts just created by the player update; decisions are returned for these
     * @param contextPosts recent unposted posts, sent as context and for summary groupings
     */
    public EditorResult decide(List<Post> newPosts, List<Post> contextPosts) {
        if (newPosts.isEmpty()) {
            return new EditorResult(new HashMap<>(), new ArrayList<>());
        }
        if (!llmClient.isEnabled()) {
            return fallback(newPosts);
        }

        int maxPosts = Math.max(1, llmProperties.getMaxPostsPerRun());
        List<Post> newCandidates = newPosts.stream().filter(this::hasStats).limit(maxPosts).toList();
        List<Post> contextCandidates = contextPosts.stream()
                .filter(this::hasStats)
                .filter(post -> newCandidates.stream().noneMatch(n -> n.getId().equals(post.getId())))
                .limit(Math.max(0, maxPosts - newCandidates.size()))
                .toList();
        if (newCandidates.isEmpty()) {
            return fallback(newPosts);
        }

        Map<Integer, Post> candidatesById = new LinkedHashMap<>();
        newCandidates.forEach(post -> candidatesById.put(post.getId(), post));
        contextCandidates.forEach(post -> candidatesById.put(post.getId(), post));
        Set<Integer> decidableIds = newCandidates.stream().map(Post::getId).collect(Collectors.toSet());

        EditorResult validated;
        try {
            String userPrompt = "Candidate posts:\n" + PostPromptFormatter.describePostsJson(newCandidates, contextCandidates);
            EditorResponse response = llmClient.generateStructured(SYSTEM_PROMPT, userPrompt, EditorResponse.class);
            validated = EditorDecisionValidator.validate(response, candidatesById, decidableIds);
        } catch (Exception ex) {
            log.atWarn().setMessage("Editor agent failed, using rule-based fallback").setCause(ex).log();
            return fallback(newPosts);
        }

        // Rule-based decisions for anything the LLM didn't validly decide (including posts over the per-run cap)
        Map<Integer, ValidatedEditorDecision> decisions = new HashMap<>(validated.decisions());
        for (Post post : newPosts) {
            if (!decisions.containsKey(post.getId())) {
                log.atInfo().setMessage("No valid editor decision for postId " + post.getId() + ", using rule-based fallback").log();
                decisions.put(post.getId(), fallbackDecision(post));
            }
        }
        log.atInfo().setMessage(String.format("Editor agent decided %d posts (%d from LLM), %d summary groups",
                decisions.size(), validated.decisions().size(), validated.summaryGroups().size())).log();
        return new EditorResult(decisions, validated.summaryGroups());
    }

    private boolean hasStats(Post post) {
        return post.getId() != null && post.getPlayerMatchPerformanceStats() != null;
    }

    EditorResult fallback(List<Post> posts) {
        Map<Integer, ValidatedEditorDecision> decisions = new HashMap<>();
        posts.forEach(post -> decisions.put(post.getId(), fallbackDecision(post)));
        return new EditorResult(decisions, new ArrayList<>());
    }

    ValidatedEditorDecision fallbackDecision(Post post) {
        List<String> highlights = performanceAnalysisService.highlightStatFields(post);
        if (highlights.isEmpty()) {
            return new ValidatedEditorDecision(post.getId(), true, EditorPostType.ALL_STAT, new ArrayList<>(),
                    "Rule-based: no standout stats", false);
        }
        List<String> featured = highlights.stream().limit(EditorDecisionValidator.MAX_FEATURED_STATS).toList();
        return new ValidatedEditorDecision(post.getId(), true, EditorPostType.STANDOUT, new ArrayList<>(featured),
                "Rule-based highlights: " + String.join(", ", featured), false);
    }
}
