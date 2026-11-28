package com.ko.footballupdater.llm.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record EditorSummaryGroup(
        @JsonPropertyDescription("Short label for the group, e.g. the team or matchday")
        String name,
        @JsonPropertyDescription("postIds of the posts to combine into one summary post, at least 2")
        List<Integer> postIds,
        @JsonPropertyDescription("Why these posts belong together")
        String reason
) {
}
