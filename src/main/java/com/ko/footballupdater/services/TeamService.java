package com.ko.footballupdater.services;

import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.TeamRepository;
import com.ko.footballupdater.repositories.UpdateStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UpdateStatusRepository updateStatusRepository;

    @Autowired
    private ParsingService parsingService;

    public Team addTeam(Team newTeam) {
        newTeam.setCheckedStatus(new CheckedStatus(DataSourceSiteName.FBREF));
        return teamRepository.save(newTeam);
    }

    public Iterable<Team> getTeams() {
        return teamRepository.findAll();
    }

    public void updateData() {
        // Find latest match data for team
        Team team = teamRepository.findAll().iterator().next();
        String matchLink = parsingService.parseTeamData(team);
        if (matchLink == null || matchLink.isEmpty()) {
            return;
        }

        //
//        List<Player> players = playerRepository.findByTeamId(team.getId());

        // Find matching players in lineup
//        parsingService.parseMatchDataForTeam(team, players, matchLink);
        team.getCheckedStatus().setLatestCheckedMatchUrl(matchLink);
        CheckedStatus checkedStatus = updateStatusRepository.save(team.getCheckedStatus());
    }
}
