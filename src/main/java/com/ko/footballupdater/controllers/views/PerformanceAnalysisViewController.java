package com.ko.footballupdater.controllers.views;

import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.form.PerformanceHighlightsDto;
import com.ko.footballupdater.services.PerformanceAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping(path="/performance")
public class PerformanceAnalysisViewController {

    @Autowired
    private PerformanceAnalysisService performanceAnalysisService;

    /**
     * Display highlight stats from recent matches
     * @return performanceHighlights view
     */
    @GetMapping("/recent-highlights")
    public String getRecentMatchHighlights(Model model) {
        PerformanceHighlightsDto performanceHighlightsDto = new PerformanceHighlightsDto();
        performanceHighlightsDto.setPostsWithHighlights(performanceAnalysisService.checkMatchPerformances());
        model.addAttribute("data", performanceHighlightsDto);
        return "performanceHighlights";
    }
}
