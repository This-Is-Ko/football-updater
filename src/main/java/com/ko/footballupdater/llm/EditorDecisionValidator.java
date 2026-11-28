package com.ko.footballupdater.llm;

import com.ko.footballupdater.llm.models.EditorPostDecision;
import com.ko.footballupdater.llm.models.EditorPostType;
import com.ko.footballupdater.llm.models.EditorResponse;
import com.ko.footballupdater.llm.models.EditorResult;
import com.ko.footballupdater.llm.models.EditorSummaryGroup;
import com.ko.footballupdater.llm.models.ValidatedEditorDecision;
import com.ko.footballupdater.llm.models.ValidatedSummaryGroup;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.utils.StatFieldHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates the editor agent's output against the candidate posts. Invalid entries are dropped and logged.
 */
@Slf4j
public final class EditorDecisionValidator {

    public static final int MAX_FEATURED_STATS = 5;

    static final String REMOVED_REASON = "[Reason removed: it contained numbers not found in the stats]";

    private EditorDecisionValidator() {
    }

    /**
     * @param response        raw LLM response
     * @param candidates      all posts sent to the model, by id
     * @param decidablePostIds posts the decisions may apply to (the new posts); others are context only
     */
    public static EditorResult validate(EditorResponse response, Map<Integer, Post> candidates, Set<Integer> decidablePostIds) {
        Map<Integer, ValidatedEditorDecision> decisions = new HashMap<>();
        List<ValidatedSummaryGroup> groups = new ArrayList<>();
        if (response == null) {
            return new EditorResult(decisions, groups);
        }

        Set<Integer> excludedPostIds = new LinkedHashSet<>();
        if (response.posts() != null) {
            for (EditorPostDecision decision : response.posts()) {
                ValidatedEditorDecision validated = validateDecision(decision, candidates, decidablePostIds, decisions);
                if (validated != null) {
                    decisions.put(validated.postId(), validated);
                    if (!validated.include()) {
                        excludedPostIds.add(validated.postId());
                    }
                }
            }
        }

        if (response.summaryGroups() != null) {
            for (EditorSummaryGroup group : response.summaryGroups()) {
                ValidatedSummaryGroup validated = validateGroup(group, candidates, excludedPostIds);
                if (validated != null) {
                    groups.add(validated);
                }
            }
        }
        return new EditorResult(decisions, groups);
    }

    private static ValidatedEditorDecision validateDecision(EditorPostDecision decision,
                                                            Map<Integer, Post> candidates,
                                                            Set<Integer> decidablePostIds,
                                                            Map<Integer, ValidatedEditorDecision> existing) {
        if (decision == null || decision.postId() == null) {
            log.atWarn().setMessage("Editor decision dropped: missing postId").log();
            return null;
        }
        Integer postId = decision.postId();
        Post post = candidates.get(postId);
        if (post == null) {
            log.atWarn().setMessage("Editor decision dropped: unknown postId " + postId).log();
            return null;
        }
        if (!decidablePostIds.contains(postId)) {
            log.atDebug().setMessage("Editor decision ignored for context post " + postId).log();
            return null;
        }
        if (existing.containsKey(postId)) {
            log.atWarn().setMessage("Editor decision dropped: duplicate postId " + postId).log();
            return null;
        }
        if (decision.include() == null) {
            log.atWarn().setMessage("Editor decision dropped: include missing for postId " + postId).log();
            return null;
        }
        if (decision.postType() == null) {
            log.atWarn().setMessage("Editor decision dropped: postType missing for postId " + postId).log();
            return null;
        }

        Map<String, Object> stats = StatFieldHelper.nonNullStats(post.getPlayerMatchPerformanceStats());
        LinkedHashSet<String> featured = new LinkedHashSet<>();
        if (decision.featuredStats() != null) {
            for (String statName : decision.featuredStats()) {
                if (statName == null || !stats.containsKey(statName)) {
                    log.atWarn().setMessage("Editor featured stat dropped: '" + statName + "' is not a non-null stat on postId " + postId).log();
                    continue;
                }
                featured.add(statName);
            }
        }
        if (featured.size() > MAX_FEATURED_STATS) {
            log.atWarn().setMessage("Editor decision dropped: " + featured.size() + " featured stats for postId " + postId + " (max " + MAX_FEATURED_STATS + ")").log();
            return null;
        }
        if (decision.include() && EditorPostType.STANDOUT.equals(decision.postType()) && featured.isEmpty()) {
            log.atWarn().setMessage("Editor decision dropped: STANDOUT without valid featured stats for postId " + postId).log();
            return null;
        }

        String reason = decision.reason() != null ? decision.reason().trim() : "";
        List<String> ungrounded = GroundingValidator.findUngroundedNumbers(reason, StatFieldHelper.allowedNumbers(post));
        if (!ungrounded.isEmpty()) {
            log.atWarn().setMessage("Editor reason replaced for postId " + postId + ", ungrounded numbers: " + ungrounded).log();
            reason = REMOVED_REASON;
        }

        return new ValidatedEditorDecision(postId, decision.include(), decision.postType(), new ArrayList<>(featured), reason, true);
    }

    private static ValidatedSummaryGroup validateGroup(EditorSummaryGroup group, Map<Integer, Post> candidates, Set<Integer> excludedPostIds) {
        if (group == null || group.postIds() == null) {
            return null;
        }
        LinkedHashSet<Integer> memberIds = new LinkedHashSet<>();
        for (Integer postId : group.postIds()) {
            if (postId == null || !candidates.containsKey(postId)) {
                log.atWarn().setMessage("Editor summary group member dropped: unknown postId " + postId).log();
            } else if (excludedPostIds.contains(postId)) {
                log.atWarn().setMessage("Editor summary group member dropped: postId " + postId + " was excluded").log();
            } else {
                memberIds.add(postId);
            }
        }
        if (memberIds.size() < 2) {
            log.atWarn().setMessage("Editor summary group dropped: fewer than 2 valid posts").log();
            return null;
        }
        Set<String> allowedNumbers = StatFieldHelper.allowedNumbers(memberIds.stream().map(candidates::get).collect(Collectors.toList()));
        String name = group.name() != null ? group.name().trim() : "";
        if (!GroundingValidator.findUngroundedNumbers(name, allowedNumbers).isEmpty()) {
            name = "";
        }
        String reason = group.reason() != null ? group.reason().trim() : "";
        if (!GroundingValidator.findUngroundedNumbers(reason, allowedNumbers).isEmpty()) {
            reason = REMOVED_REASON;
        }
        return new ValidatedSummaryGroup(name, new ArrayList<>(memberIds), reason);
    }
}
