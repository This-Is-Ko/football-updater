package com.ko.footballupdater.services;

import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.TeamRepository;
import com.ko.footballupdater.repositories.UpdateStatusRepository;
import com.ko.footballupdater.responses.AddNewTeamResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UpdateStatusRepository updateStatusRepository;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private ParsingService parsingService;

    public void addTeam(Team newTeam, AddNewTeamResponse response) throws Exception {
        if (!teamRepository.findByNameAndCountryAndLeague(newTeam.getName(), newTeam.getCountry(), newTeam.getLeague()).isEmpty()) {
            throw new Exception("Team already exists");
        }

        newTeam.setCheckedStatus(new CheckedStatus(DataSourceSiteName.FBREF));
        Team savedTeam = teamRepository.save(newTeam);
        response.setTeam(savedTeam);
        // Parse players and add to database
        List<Player> players = parsingService.parseSquadDataForTeam(newTeam);
        if (players == null || players.isEmpty()) {
            log.info("No players to add");
            return;
        }
        for (Player player : players) {
            try {
                playerService.addPlayer(player);
                response.getPlayersAdded().add(player);
            } catch (Exception ex) {
                log.info("Error while adding player " + player.getName());
            }
        }
        log.info("Added " + response.getPlayersAdded().size() + " players");
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
