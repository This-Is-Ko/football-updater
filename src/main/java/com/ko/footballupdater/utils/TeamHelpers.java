package com.ko.footballupdater.utils;

import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.repositories.TeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
public class TeamHelpers {

    @Autowired
    TeamRepository teamRepository;

    public Team findTeamByNameOrAlternativeName(String name) {
        List<Team> teams = teamRepository.findByName(name);
        if (teams.isEmpty()) {
            teams = teamRepository.findByAlternativeTeamName(name);
        }
        if (teams != null && teams.size() == 1) {
            return teams.get(0);
        }
        return null;
    }
}
