package com.ko.footballupdater.llm.models;

import java.util.List;

/**
 * An editor decision for one post that passed validation (or came from the rule-based fallback).
 */
public record ValidatedEditorDecision(
        Integer postId,
        boolean include,
        EditorPostType postType,
        List<String> featuredStats,
        String reason,
        boolean fromLlm
) {
}
