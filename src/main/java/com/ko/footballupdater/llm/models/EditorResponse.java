package com.ko.footballupdater.llm.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record EditorResponse(
        @JsonPropertyDescription("One decision per candidate post")
        List<EditorPostDecision> posts,
        @JsonPropertyDescription("Optional groupings of posts that would work as a summary post. Empty if none")
        List<EditorSummaryGroup> summaryGroups
) {
}
