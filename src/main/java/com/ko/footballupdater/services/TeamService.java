package com.ko.footballupdater.services;

import com.amazonaws.services.kms.model.NotFoundException;
import com.ko.footballupdater.models.AlternativeName;
import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.Hashtag;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.TeamRepository;
import com.ko.footballupdater.repositories.UpdateStatusRepository;
import com.ko.footballupdater.request.AddTeamRequest;
import com.ko.footballupdater.request.AddTeamRequestDataSource;
import com.ko.footballupdater.request.UpdateTeamRequest;
import com.ko.footballupdater.responses.AddNewTeamResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Iterable<Team> getTeams() {
        return teamRepository.findAll();
    }

    public void addTeam(AddTeamRequest addTeamRequest, AddNewTeamResponse response) throws IllegalArgumentException {
        // Check if team exists
        if (!teamRepository.findByName(addTeamRequest.getName()).isEmpty()) {
            throw new IllegalArgumentException("Team already exists");
        }

        // Construct team object from request
        Team newTeam = new Team();
        newTeam.setName(addTeamRequest.getName());
        if (addTeamRequest.getAdditionalHashtags() != null) {
            Set<Hashtag> additionalHashtags = new HashSet<>();
            for (String hashtag : addTeamRequest.getAdditionalHashtags()) {
               additionalHashtags.add(new Hashtag(hashtag));
            }
            newTeam.setAdditionalHashtags(additionalHashtags);
        }
        if (addTeamRequest.getAlternativeNames() != null) {
            Set<AlternativeName> alternativeNames = new HashSet<>();
            for (String requestAltName : addTeamRequest.getAlternativeNames()) {
                alternativeNames.add(new AlternativeName(requestAltName));
            }
            newTeam.setAlternativeNames(alternativeNames);
        }
        if (addTeamRequest.getDataSources() != null) {
            Set<DataSource> dataSources = new HashSet<>();
            for (AddTeamRequestDataSource requestDataSource : addTeamRequest.getDataSources()) {
                dataSources.add(new DataSource(requestDataSource.getType(), requestDataSource.getSiteName(), requestDataSource.getUrl()));
            }
            newTeam.setDataSources(dataSources);
        }

        Team savedTeam = teamRepository.save(newTeam);
        log.info("Saved new team " + savedTeam.getName());
        response.setTeam(savedTeam);

        // Parse players and add to database
        if (addTeamRequest.getPopulatePlayers() != null && addTeamRequest.getPopulatePlayers()) {
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
                    response.getPlayersNotAdded().add(player);
                    log.info("Error while adding player " + player.getName(), ex);
                }
            }
            log.info("Added " + response.getPlayersAdded().size() + " players");
        }
    }

    public void updateTeam(Integer teamId, UpdateTeamRequest updateTeamRequest, AddNewTeamResponse response) throws NotFoundException {
        // Find team
        Optional<Team> teamSearchResult = teamRepository.findById(teamId);
        if (teamSearchResult.isEmpty()) {
            throw new NotFoundException("Team can't be found");
        }
        Team team = teamSearchResult.get();

        if (updateTeamRequest.getDataSources() != null && !updateTeamRequest.getDataSources().isEmpty()) {
            Set<DataSource> dataSources = !team.getDataSources().isEmpty() ? team.getDataSources() : new HashSet<>();
            for (AddTeamRequestDataSource requestDataSource : updateTeamRequest.getDataSources()) {
                dataSources.add(new DataSource(requestDataSource.getType(), requestDataSource.getSiteName(), requestDataSource.getUrl()));
            }
            team.setDataSources(dataSources);
        }
        if (updateTeamRequest.getAlternativeNames() != null) {
            Set<AlternativeName> alternativeNames = !team.getAlternativeNames().isEmpty() ? team.getAlternativeNames() : new HashSet<>();
            Set<AlternativeName> newAlternativeNames = updateTeamRequest.getAlternativeNames().stream()
                    .filter(name -> !name.isEmpty())
                    .map(AlternativeName::new)
                    .collect(Collectors.toSet());
            alternativeNames.addAll(newAlternativeNames);
            team.setAlternativeNames(alternativeNames);
        }
        if (updateTeamRequest.getAdditionalHashtags() != null) {
            Set<Hashtag> additionalHashtags = !team.getAdditionalHashtags().isEmpty() ? team.getAdditionalHashtags() : new HashSet<>();
            Set<Hashtag> newAlternativeNames = updateTeamRequest.getAdditionalHashtags().stream()
                    .filter(hashtag -> !hashtag.isEmpty())
                    .map(Hashtag::new)
                    .collect(Collectors.toSet());
            additionalHashtags.addAll(newAlternativeNames);
            team.setAdditionalHashtags(additionalHashtags);
        }
        Team updatedTeam = teamRepository.save(team);
        log.info("Updated team" + updatedTeam.getName());
        response.setTeam(updatedTeam);
    }

    public void deleteTeam(Integer teamId) throws IllegalArgumentException {
        boolean isFound = teamRepository.existsById(teamId);
        if (!isFound) {
            throw new IllegalArgumentException("Team doesn't exist");
        }
        teamRepository.deleteById(teamId);
    }

    // TODO
//    public void updateTeamPlayers(Team newTeam, UpdateTeamPlayersResponse response) throws Exception {
//        // Check if team exists
//        List<Team> searchResult = teamRepository.findByNameAndCountryAndLeague(newTeam.getName(), newTeam.getCountry(), newTeam.getLeague());
//        if (searchResult.isEmpty()) {
//            throw new Exception("Team doesn't exist");
//        } else if (searchResult.size() > 1) {
//            throw new Exception("Too many teams returned from search");
//        }
//
//        Team team = searchResult.get(0);
//        // Parse players and add to database
//        List<Player> players = parsingService.parseSquadDataForTeam(newTeam);
//        if (players == null || players.isEmpty()) {
//            log.info("No players to add");
//            return;
//        }
//        for (Player player : players) {
//            try {
//                playerService.addPlayer(player);
//                response.getPlayersAdded().add(player);
//            } catch (Exception ex) {
//                log.info("Error while adding player " + player.getName());
//            }
//        }
//        log.info("Added " + response.getPlayersAdded().size() + " players");
//    }



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
