package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.Post;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PerformanceHighlightsDto {
    private Map<Post, Map<String, Integer>> postsWithHighlights;
}
