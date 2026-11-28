package com.ko.footballupdater.llm.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record EditorPostDecision(
        @JsonPropertyDescription("The postId of the candidate post this decision is for")
        Integer postId,
        @JsonPropertyDescription("Whether the post is worth publishing")
        Boolean include,
        @JsonPropertyDescription("ALL_STAT for a standard post, STANDOUT to feature a few stats on a standout image, SUMMARY_MEMBER if it only belongs in a matchday summary")
        EditorPostType postType,
        @JsonPropertyDescription("Stat field names to feature, copied exactly from the post's stats. At most 5. Required for STANDOUT")
        List<String> featuredStats,
        @JsonPropertyDescription("One or two sentences explaining the decision for the human reviewer. Only use numbers that appear in the post's stats")
        String reason
) {
}
