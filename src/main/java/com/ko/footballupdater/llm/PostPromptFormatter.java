package com.ko.footballupdater.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.utils.DateTimeHelper;
import com.ko.footballupdater.utils.StatFieldHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns posts into the structured JSON the agents see. Only data from the database goes in here.
 */
public final class PostPromptFormatter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private PostPromptFormatter() {
    }

    public static Map<String, Object> describePost(Post post, boolean isNew) {
        Map<String, Object> description = new LinkedHashMap<>();
        description.put("postId", post.getId());
        description.put("isNew", isNew);
        description.put("player", post.getPlayer() != null ? post.getPlayer().getName() : null);
        description.put("team", relevantTeam(post));
        description.put("match", describeMatch(post));
        description.put("stats", StatFieldHelper.nonNullStats(post.getPlayerMatchPerformanceStats()));
        return description;
    }

    public static Map<String, Object> describeMatch(Post post) {
        Map<String, Object> match = new LinkedHashMap<>();
        if (post.getPlayerMatchPerformanceStats() == null || post.getPlayerMatchPerformanceStats().getMatch() == null) {
            return match;
        }
        Match m = post.getPlayerMatchPerformanceStats().getMatch();
        match.put("homeTeam", m.getHomeTeamName());
        match.put("awayTeam", m.getAwayTeamName());
        match.put("homeTeamScore", m.getHomeTeamScore());
        match.put("awayTeamScore", m.getAwayTeamScore());
        match.put("date", m.getDate() != null ? DateTimeHelper.getDateAsFormattedString(m.getDate()) : null);
        return match;
    }

    public static String relevantTeam(Post post) {
        if (post.getPlayerMatchPerformanceStats() != null
                && post.getPlayerMatchPerformanceStats().getMatch() != null
                && post.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam() != null) {
            return post.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam();
        }
        if (post.getPlayer() != null && post.getPlayer().getTeam() != null) {
            return post.getPlayer().getTeam().getName();
        }
        return null;
    }

    /**
     * @return the team the player played against, or null if it can't be worked out
     */
    public static String opponentTeam(Post post) {
        String team = relevantTeam(post);
        if (team == null || post.getPlayerMatchPerformanceStats() == null || post.getPlayerMatchPerformanceStats().getMatch() == null) {
            return null;
        }
        Match match = post.getPlayerMatchPerformanceStats().getMatch();
        if (matchesTeam(match.getHomeTeamName(), team)) {
            return match.getAwayTeamName();
        }
        if (matchesTeam(match.getAwayTeamName(), team)) {
            return match.getHomeTeamName();
        }
        return null;
    }

    private static boolean matchesTeam(String matchTeamName, String team) {
        if (matchTeamName == null) {
            return false;
        }
        String a = matchTeamName.toLowerCase();
        String b = team.toLowerCase();
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialise prompt data", ex);
        }
    }

    public static String describePostsJson(List<Post> newPosts, List<Post> contextPosts) {
        List<Map<String, Object>> candidates = new java.util.ArrayList<>();
        newPosts.forEach(post -> candidates.add(describePost(post, true)));
        contextPosts.forEach(post -> candidates.add(describePost(post, false)));
        return toJson(candidates);
    }
}
