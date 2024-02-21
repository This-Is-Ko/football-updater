package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Player;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlayersDto {
    private List<Player> players;
}
