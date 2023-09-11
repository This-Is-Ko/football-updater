package com.ko.footballupdater.services;

import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.models.UpdateStatus;
import com.ko.footballupdater.repositories.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ParsingService parsingService;

    public Team addTeam(Team newTeam) {
        newTeam.setUpdateStatus(new UpdateStatus(DataSourceSiteName.FBREF));
        return teamRepository.save(newTeam);
    }

    public Iterable<Team> getTeams() {
        return teamRepository.findAll();
    }

    public void updateData() {
        Team test = teamRepository.findAll().iterator().next();
        parsingService.parseTeamData(test);
    }
}
