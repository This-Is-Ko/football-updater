package com.ko.footballupdater.services;

import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.InstagramPost;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.UpdateStatusRepository;
import com.ko.footballupdater.responses.UpdatePlayersResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UpdateStatusRepository updateStatusRepository;

    @Autowired
    private ParsingService parsingService;

    @Autowired
    private EmailService emailService;

    public Player addPlayer(Player newPlayer) throws Exception {
        if (!playerRepository.findByNameEquals(newPlayer.getName()).isEmpty()) {
            throw new Exception("Player already exists");
        }

        newPlayer.setCheckedStatus(new CheckedStatus(DataSourceSiteName.FBREF));
        return playerRepository.save(newPlayer);
    }

    public Iterable<Player> getPlayers() {
        return playerRepository.findAll();
    }

    public void updateDataForAllPlayers(UpdatePlayersResponse response) {
        // Find latest match data for each player
        Iterator<Player> playerIterator = playerRepository.findAll().iterator();
        List<InstagramPost> posts = new ArrayList<>();

        while(playerIterator.hasNext()){
            Player player = playerIterator.next();
            PlayerMatchPerformanceStats playerMatchPerformanceStats = parsingService.parsePlayerMatchData(player);
            if (playerMatchPerformanceStats == null) {
                // No new updates
                continue;
            }
            // Generate caption
            InstagramPost post = new InstagramPost(player, playerMatchPerformanceStats);
            posts.add(post);
        }

        // No updates
        if (posts.isEmpty()) {
            return;
        }

        boolean isEmailSent = emailService.sendEmailUpdate(posts);
        response.setEmailSent(isEmailSent);
        if (isEmailSent) {
            List<Player> playersToUpdate = new ArrayList<>();
            Date currentDateTime = new Date();
            // Update player checked status if email was sent
            for (InstagramPost post : posts) {
                post.getPlayer().getCheckedStatus().setLastChecked(currentDateTime);
                post.getPlayer().getCheckedStatus().setLatestCheckedMatchUrl(post.getPlayerMatchPerformanceStats().getMatch().getUrl());
                playersToUpdate.add(post.getPlayer());
            }
            playerRepository.saveAll(playersToUpdate);
            // Populate response
            response.setPlayersUpdated(playersToUpdate);
            response.setNumPlayersUpdated(playersToUpdate.size());
        }
    }
}
