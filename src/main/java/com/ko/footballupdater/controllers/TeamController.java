package com.ko.footballupdater.controllers;


import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.services.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(path="/team")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PostMapping(path="/add")
    public @ResponseBody Team addNewTeam(@RequestBody Team newTeam) {
        return teamService.addTeam(newTeam);
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<Team> getAllTeams() {
        return teamService.getTeams();
    }

    @GetMapping(path="/data/update")
    public @ResponseBody String dataUpdate() {
        teamService.updateData();
        return "A";
    }
}