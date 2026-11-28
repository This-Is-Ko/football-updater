package com.ko.footballupdater.llm.models;

import java.util.List;

public record ValidatedSummaryGroup(String name, List<Integer> postIds, String reason) {
}
