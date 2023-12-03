package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.repositories.UpdateStatusRepository;
import com.ko.footballupdater.responses.UpdatePlayersResponse;
import com.ko.footballupdater.utils.PostHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private UpdateStatusRepository updateStatusRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ParsingService parsingService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AmazonS3Service amazonS3Service;

    @Autowired
    private ImageGeneratorService imageGeneratorService;

    @Autowired
    private InstagramPostProperies instagramPostProperies;

    public Player addPlayer(Player newPlayer, DataSourceSiteName dataSourceSiteName) throws Exception {
        if (!playerRepository.findByNameEquals(newPlayer.getName()).isEmpty()) {
            throw new Exception("Player already exists");
        }

        newPlayer.setCheckedStatus(new CheckedStatus(dataSourceSiteName));
        return playerRepository.save(newPlayer);
    }

    public Player addPlayer(Player newPlayer) throws Exception {
        // Default to FOTMOB
        return addPlayer(newPlayer, DataSourceSiteName.FOTMOB);
    }

    public Iterable<Player> getPlayers() {
        return playerRepository.findAll();
    }

    public UpdatePlayersResponse updateDataForAllPlayers() {
        // Find latest match data for each player
        Iterator<Player> playerIterator = playerRepository.findAll().iterator();
        List<Player> requestPlayersToUpdate = new ArrayList<>();

        while(playerIterator.hasNext()){
            Player player = playerIterator.next();
            requestPlayersToUpdate.add(player);
        }

        return updateDataForPlayers(requestPlayersToUpdate);
    }

    public UpdatePlayersResponse updateDataForPlayer(Integer playerId) throws Exception {
        // Find latest match data for individual player
        Optional<Player> requestPlayersToUpdate = playerRepository.findById(playerId);
        if (requestPlayersToUpdate.isEmpty()) {
            throw new Exception("Player name not found");
        }
        return updateDataForPlayers(requestPlayersToUpdate.stream().toList());
    }

    public UpdatePlayersResponse updateDataForPlayers(List<Player> requestPlayersToUpdate) {
        UpdatePlayersResponse response = new UpdatePlayersResponse();

        // Find latest match data for each player
        List<Post> posts = new ArrayList<>();

        for (Player player : requestPlayersToUpdate) {
            PlayerMatchPerformanceStats playerMatchPerformanceStats = parsingService.parsePlayerMatchData(player);
            if (playerMatchPerformanceStats == null) {
                // No new updates
                continue;
            }
            // Generate post and caption
            Post post = new Post(PostType.ALL_STAT_POST, player, playerMatchPerformanceStats);
            try {
                // Generate stat images
                imageGeneratorService.generatePlayerStatImage(post);
                // Upload stat images to s3
                amazonS3Service.uploadToS3(post);
                // Generate caption
                PostHelper.generatePostCaption(instagramPostProperies.getVersion(), post, instagramPostProperies.getDefaultHashtags());
                // Generate image search links
                PostHelper.generatePostImageSearchUrl(post);
            } catch (Exception e) {
                // Skip if image generation or upload fails, allows future retry
                log.warn(post.getPlayer().getName() + " - Unable to generate or upload image");
                continue;
            }
            posts.add(post);
        }

        // No updates
        if (posts.isEmpty()) {
            return response;
        }

        // Attempt to send email with updates
        boolean isEmailSent = emailService.sendEmailUpdate(posts);

        response.setEmailSent(isEmailSent);
        // Update player checked status regardless of email status
        List<Player> playersToUpdate = new ArrayList<>();
        Date currentDateTime = new Date();
        for (Post post : posts) {
            // Save post in database for dashboard use
            postRepository.save(post);

            post.getPlayer().getCheckedStatus().setLastChecked(currentDateTime);
            post.getPlayer().getCheckedStatus().setLatestCheckedMatchUrl(post.getPlayerMatchPerformanceStats().getMatch().getUrl());
            post.getPlayer().getCheckedStatus().setLatestCheckedMatchDate(post.getPlayerMatchPerformanceStats().getMatch().getDate());
            playersToUpdate.add(post.getPlayer());
        }
        playerRepository.saveAll(playersToUpdate);
        // Populate response
        response.setPlayersUpdated(playersToUpdate);
        response.setNumPlayersUpdated(playersToUpdate.size());
        return response;
    }
}
