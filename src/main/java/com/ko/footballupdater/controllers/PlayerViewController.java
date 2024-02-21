package com.ko.footballupdater.controllers;

import com.ko.footballupdater.models.form.PlayersDto;
import com.ko.footballupdater.services.PlayerViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping(path="/players")
public class PlayerViewController {

    @Autowired
    private PlayerViewService playerViewService;

    /**
     * Display all players
     * @return players view
     */
    @GetMapping("")
    public String getPlayers(Model model) {
        PlayersDto playersDto = playerViewService.getPlayers();

        model.addAttribute("data", playersDto);
        return "players";
    }
}
