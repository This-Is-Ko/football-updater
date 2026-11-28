package com.ko.footballupdater.llm.models;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record CaptionResponse(
        @JsonPropertyDescription("The caption body without hashtags, using the {placeholders} for names")
        String caption
) {
}
