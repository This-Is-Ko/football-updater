package com.ko.footballupdater.responses;

import com.ko.footballupdater.models.Player;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePlayersResponse {

    private List<Player> playersUpdated;

    private int numPlayersUpdated = 0;

    private boolean isEmailSent = false;
}
