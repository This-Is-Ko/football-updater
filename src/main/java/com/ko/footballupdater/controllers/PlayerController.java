package com.ko.footballupdater.controllers;

import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.services.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping(path="/player")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    @PostMapping(path="/add")
    public @ResponseBody Player addNewPlayer(@RequestBody Player newPlayer) {
        try {
            return playerService.addPlayer(newPlayer);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unable to add player", ex);
        }
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<Player> getAllPlayers() {
        return playerService.getPlayers();
    }

    @GetMapping(path="/data/update")
    public @ResponseBody String dataUpdate() {
        try {
            int updatedPlayers = playerService.updateDataForAllPlayers();
            return updatedPlayers + " player(s) updated";
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Updating players failed", ex);
        }
    }

}