package com.ko.footballupdater.controllers;


import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.repositories.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(path="/main")
public class MainController {
    @Autowired
    private PlayerRepository playerRepository;

//    @PostMapping(path="/add")
//    public @ResponseBody String addNewUser (@RequestParam String name
//            , @RequestParam String email) {
//
//        Player n = new Player();
//        n.setName(name);
//        playerRepository.save(n);
//        return "Saved";
//    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<Player> getAllUsers() {
        return playerRepository.findAll();
    }
}