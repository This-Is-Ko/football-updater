package com.ko.footballupdater.controllers.views;

import com.ko.footballupdater.models.form.TeamsDto;
import com.ko.footballupdater.services.TeamViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping(path="/teams")
public class TeamViewController {

    @Autowired
    private TeamViewService teamViewService;

    /**
     * Display all teams
     * @return teams view
     */
    @GetMapping("")
    public String getTeams(Model model) {
        TeamsDto teamsDto = teamViewService.getTeams();

        model.addAttribute("data", teamsDto);
        return "teams";
    }
}
