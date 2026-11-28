package com.ko.footballupdater.llm.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record QaVerdict(
        @JsonPropertyDescription("True if the post looks ready to publish")
        Boolean pass,
        @JsonPropertyDescription("Problems found, empty if none")
        List<QaIssue> issues
) {
}
