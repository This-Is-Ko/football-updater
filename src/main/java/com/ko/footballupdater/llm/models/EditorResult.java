package com.ko.footballupdater.llm.models;

import java.util.List;
import java.util.Map;

/**
 * Validated editor output, keyed by post id.
 */
public record EditorResult(Map<Integer, ValidatedEditorDecision> decisions, List<ValidatedSummaryGroup> summaryGroups) {
}
