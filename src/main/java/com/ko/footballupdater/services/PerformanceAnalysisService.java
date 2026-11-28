package com.ko.footballupdater.services;

import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.repositories.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PerformanceAnalysisService {

    @Autowired
    private PostRepository postRepository;

    public Map<Post, Map<String, Integer>> checkMatchPerformances() {
        List<Post> recentPosts = postRepository.findAllByOrderByDateGeneratedDesc(PageRequest.of(0, 20));

        Map<Post, Map<String, Integer>> postsWithHighlights = new HashMap<>();
        // Check stats in each recent post for highlights
        for (Post post : recentPosts) {
            if (PostType.SUMMARY_POST.equals(post.getPostType())) {
                continue;
            }
            Map<String, Integer> highlights = checkHighlightsInMatch(post);
            if (!highlights.isEmpty()) {
                postsWithHighlights.put(post, highlights);
            }
        }
        return postsWithHighlights;
    }

    public Map<String, Integer> checkHighlightsInMatch(Post post) {
        if (post == null || post.getPlayerMatchPerformanceStats() == null) {
            throw new RuntimeException("Missing performance stats");
        }
        PlayerMatchPerformanceStats matchPerformanceStats = post.getPlayerMatchPerformanceStats();
        Map<String, Integer> highlights = new HashMap<>();
        // Goals
        if (matchPerformanceStats.getGoals() != null && matchPerformanceStats.getGoals() > 0) {
            highlights.put("goal(s)", matchPerformanceStats.getGoals());
        }
        // Assists
        if (matchPerformanceStats.getAssists() != null && matchPerformanceStats.getAssists() > 0) {
            highlights.put("assist(s)", matchPerformanceStats.getAssists());
        }
        // Passing accuracy
        if (matchPerformanceStats.getPassesSuccessPercentage() != null
                && matchPerformanceStats.getPassesAttempted() != null
                && matchPerformanceStats.getPassesSuccessPercentage() > 80
                && matchPerformanceStats.getPassesAttempted() > 40) {
            highlights.put("successful passing (%)", matchPerformanceStats.getPassesSuccessPercentage());
            highlights.put("successful passes", matchPerformanceStats.getPassesAttempted());
        }
        return highlights;
    }

    /**
     * Same rules as {@link #checkHighlightsInMatch(Post)}, returning the stat field names.
     * Used as the fallback when the LLM editor is disabled or fails.
     */
    public List<String> highlightStatFields(Post post) {
        List<String> fields = new ArrayList<>();
        if (post == null || post.getPlayerMatchPerformanceStats() == null) {
            return fields;
        }
        PlayerMatchPerformanceStats stats = post.getPlayerMatchPerformanceStats();
        if (stats.getGoals() != null && stats.getGoals() > 0) {
            fields.add("goals");
        }
        if (stats.getAssists() != null && stats.getAssists() > 0) {
            fields.add("assists");
        }
        if (stats.getPassesSuccessPercentage() != null
                && stats.getPassesAttempted() != null
                && stats.getPassesSuccessPercentage() > 80
                && stats.getPassesAttempted() > 40) {
            fields.add("passesSuccessPercentage");
            fields.add("passesAttempted");
        }
        return fields;
    }
}
