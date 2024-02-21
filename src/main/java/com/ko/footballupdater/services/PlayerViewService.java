package com.ko.footballupdater.services;


import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.form.PlayersDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class PlayerViewService {

    @Autowired
    private PlayerService playerService;

    public PlayersDto getPlayers() {
        Iterable<Player> players = playerService.getPlayers();

        List<Player> playersList = new ArrayList<>();
        players.forEach(playersList::add);

        // Sort alphabetically
        playersList.sort(Comparator.comparing(Player::getName));

        PlayersDto playersDto = new PlayersDto();
        playersDto.setPlayers(playersList);
        return playersDto;
    }
}
