package com.ko.footballupdater.services;


import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.models.form.TeamsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TeamViewService {


    @Autowired
    private TeamService teamService;

    public TeamsDto getTeams() {
        Iterable<Team> teams = teamService.getTeams();

        List<Team> teamsList = new ArrayList<>();
        teams.forEach(teamsList::add);

        TeamsDto teamsDto = new TeamsDto();
        teamsDto.setTeams(teamsList);
        return teamsDto;
    }
}
