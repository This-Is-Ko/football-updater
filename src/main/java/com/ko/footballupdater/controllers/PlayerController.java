package com.ko.footballupdater.controllers;

import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.request.UpdatePlayerRequest;
import com.ko.footballupdater.request.UpdateTeamRequest;
import com.ko.footballupdater.responses.AddNewTeamResponse;
import com.ko.footballupdater.responses.UpdatePlayersResponse;
import com.ko.footballupdater.services.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping(path="/api/player")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    /**
     * Create new player entry
     * @return created player entry
     */
    @PostMapping(path="/add")
    public @ResponseBody Player addNewPlayer(@RequestBody Player newPlayer) {
        try {
            return playerService.addPlayer(newPlayer);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unable to add player", ex);
        }
    }

    /**
     * Update player entry based on id
     * @return updated player entry
     */
    @PatchMapping(path="/{playerId}")
    public @ResponseBody ResponseEntity<Player> updatePlayer(
            @PathVariable("playerId") Integer playerId,
            @RequestBody UpdatePlayerRequest updatePlayerRequest) {
        try {
            return ResponseEntity.ok(playerService.updatePlayer(playerId, updatePlayerRequest));
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Updating player failed", ex);
        }
    }

    /**
     * Get all players in database
     * @return All player entries
     */
    @GetMapping(path="/all")
    public @ResponseBody Iterable<Player> getAllPlayers() {
        return playerService.getPlayers();
    }

    @GetMapping(path="/data/update")
    public @ResponseBody ResponseEntity<UpdatePlayersResponse> dataUpdateForAllPlayers() {
        try {
            return ResponseEntity.ok(playerService.updateDataForAllPlayers());
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Updating players failed", ex);
        }
    }

    @GetMapping(path="/data/update/{playerId}")
    public @ResponseBody ResponseEntity<UpdatePlayersResponse> dataUpdateForPlayer(
            @PathVariable("playerId") Integer playerId
    ) {
        try {
            return ResponseEntity.ok(playerService.updateDataForPlayer(playerId));
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Updating player failed", ex);
        }
    }
}
