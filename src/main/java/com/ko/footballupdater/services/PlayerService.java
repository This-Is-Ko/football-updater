package com.ko.footballupdater.services;

import com.amazonaws.services.kms.model.NotFoundException;
import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Hashtag;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.repositories.TeamRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TeamRepository teamRepository;

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

    public Player addPlayer(Player newPlayer, DataSourceSiteName dataSourceSiteName) throws IllegalArgumentException {
        if (!playerRepository.findByNameEquals(newPlayer.getName()).isEmpty()) {
            throw new IllegalArgumentException("Player already exists");
        }

        newPlayer.setCheckedStatus(new CheckedStatus(dataSourceSiteName));
        return playerRepository.save(newPlayer);
    }

    public Player addPlayer(Player newPlayer) throws IllegalArgumentException {
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

    public UpdatePlayersResponse updateDataForPlayer(Integer playerId) throws NotFoundException {
        // Find latest match data for individual player
        Optional<Player> requestPlayersToUpdate = playerRepository.findById(playerId);
        if (requestPlayersToUpdate.isEmpty()) {
            throw new NotFoundException("Player name not found");
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
                // Generate any additional team hashtags
                String hashtags = "";
                if (post.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam() != null && !post.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam().isEmpty()) {
                    hashtags = generateTeamHashtags(post.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam());
                    log.atInfo().setMessage("Set team hashtags to: " + hashtags).addKeyValue("player", player.getName()).log();
                } else {
                    log.atWarn().setMessage("Unable to generate team hashtags due to relevant team missing").addKeyValue("player", player.getName()).log();
                }
                // Generate caption
                PostHelper.generatePostCaption(instagramPostProperies.getVersion(), post, instagramPostProperies.getDefaultHashtags() + hashtags);
                log.atInfo().setMessage("Generated post caption: " + post.getCaption()).addKeyValue("player", player.getName()).log();
                // Generate image search links
                PostHelper.generatePostImageSearchUrl(post);
            } catch (Exception e) {
                // Skip if image generation or upload fails, allows future retry
                log.atWarn().setMessage("Unable to generate or upload image").addKeyValue("player", player.getName()).log();
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

    public String generateTeamHashtags(String teamName) {
        StringBuilder teamHashtags = new StringBuilder(" ");
        if (teamName == null || teamName.isEmpty()) {
            return teamHashtags.toString();
        }

        // Search for team with name
        List<Team> playerTeams = teamRepository.findByName(teamName);
        if (playerTeams.size() == 1) {
            Team playerTeam = playerTeams.get(0);
            if (playerTeam != null) {
                teamHashtags.append(
                        playerTeam.getAdditionalHashtags().stream()
                                .map(Hashtag::getValue)
                                .collect(Collectors.joining(", "))
                );
            }
        } else {
            // Search for team with name in alternative name field, if not found, generate alternative name hashtag
            List<Team> playerTeamsAltName = teamRepository.findByAlternativeName(teamName);
            if (playerTeamsAltName.size() == 1) {
                Team playerTeam = playerTeamsAltName.get(0);
                if (playerTeam != null) {
                    teamHashtags.append(
                            playerTeam.getAdditionalHashtags().stream()
                                    .map(Hashtag::getValue)
                                    .collect(Collectors.joining(" "))
                    );
                }
            } else {
                teamHashtags.append("#").append(teamName.replaceAll(" ", "").replaceAll("-", ""));
            }
        }

        return teamHashtags.toString().replaceAll(",", "");
    }
}
