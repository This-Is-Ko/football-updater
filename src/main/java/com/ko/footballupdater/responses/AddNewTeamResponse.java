package com.ko.footballupdater.responses;

import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.Team;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AddNewTeamResponse {

    private Team team;

    private List<Player> playersAdded = new ArrayList<>();

    private List<Player> playersNotAdded = new ArrayList<>();

}
