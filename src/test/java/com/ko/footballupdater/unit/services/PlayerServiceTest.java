package com.ko.footballupdater.unit.services;

import com.amazonaws.services.kms.model.NotFoundException;
import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.Hashtag;
import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.repositories.TeamRepository;
import com.ko.footballupdater.responses.UpdatePlayersResponse;
import com.ko.footballupdater.services.AmazonS3Service;
import com.ko.footballupdater.services.EmailService;
import com.ko.footballupdater.services.ImageGeneratorService;
import com.ko.footballupdater.services.ParsingService;
import com.ko.footballupdater.services.PlayerService;
import com.ko.footballupdater.utils.PostHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerServiceTest {

    @InjectMocks
    private PlayerService playerService;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ParsingService parsingService;

    @Mock
    private EmailService emailService;

    @Mock
    private AmazonS3Service amazonS3Service;

    @Mock
    private PostHelper postHelper;

    @Mock
    private ImageGeneratorService imageGeneratorService;

    @Mock
    private InstagramPostProperies instagramPostProperies;

    @Mock
    private ImageGeneratorProperties imageGeneratorProperties;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(instagramPostProperies.getVersion()).thenReturn(2);
        when(instagramPostProperies.getDefaultHashtags()).thenReturn("#default");
        when(instagramPostProperies.getAccountName()).thenReturn("Insta account name");
        when(imageGeneratorProperties.isEnabled()).thenReturn(true);
    }

    @Test
    public void addPlayer_validPlayer_successful() {
        Player newPlayer = new Player();
        newPlayer.setName("Player name");

        when(playerRepository.findByNameEquals(newPlayer.getName())).thenReturn(new ArrayList<>());
        when(playerRepository.save(newPlayer)).thenReturn(newPlayer);

        Player addedPlayer = playerService.addPlayer(newPlayer);

        assertEquals(newPlayer, addedPlayer);
    }

    @Test
    public void addPlayer_existingPlayer_throwException() {
        Player existingPlayer = new Player();
        existingPlayer.setName("John Doe");

        when(playerRepository.findByNameEquals(existingPlayer.getName())).thenReturn(List.of(existingPlayer));

        try {
            playerService.addPlayer(existingPlayer);
        } catch (IllegalArgumentException e) {
            assertEquals("Player already exists", e.getMessage());
        }
    }

    @Test
    public void updateDataForPlayer_onePlayer_successful() throws Exception {
        Integer playerId = 1;
        Player playerToUpdate = new Player("Player1");
        playerToUpdate.setCheckedStatus(new CheckedStatus());
        playerToUpdate.setId(playerId);

        PlayerMatchPerformanceStats mockPerformanceStats = mock(PlayerMatchPerformanceStats.class);
        when(parsingService.parsePlayerMatchData(playerToUpdate)).thenReturn(mockPerformanceStats);

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(playerToUpdate));

        doNothing().when(imageGeneratorService).generatePlayerStatImage(any(Post.class));
        doNothing().when(amazonS3Service).uploadToS3(any(Post.class));

        Match match = new Match("https://url", Date.from(Instant.now()), "homeTeamName", "awayTeamName", "relevantTeamName");
        when(mockPerformanceStats.getMatch()).thenReturn(match);

        when(emailService.sendEmailUpdate(anyList())).thenReturn(true);

        when(postRepository.save(any(Post.class))).thenReturn(new Post());

        when(playerRepository.saveAll(any())).thenReturn(List.of(playerToUpdate));

        when(postHelper.generateTeamHashtags("relevantTeamName")).thenReturn("#someTeamhashtag");

        UpdatePlayersResponse response = playerService.updateDataForPlayer(playerId);

        assertTrue(response.isEmailSent());
        assertEquals(1, response.getNumPlayersUpdated());
        assertNotNull(response.getPlayersUpdated());
        assertEquals(1, response.getPlayersUpdated().size());
        assertEquals(playerToUpdate, response.getPlayersUpdated().get(0));
    }

    @Test
    public void updateDataForPlayer_playerNotFound_throwNotFoundException() {
        Integer playerId = 1;
        when(playerRepository.findById(playerId)).thenReturn(Optional.empty());

        try {
            playerService.updateDataForPlayer(playerId);
        } catch (NotFoundException e) {
            assertEquals("Player name not found", e.getErrorMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateDataForAllPlayers_playersFound_successful() throws Exception {
        List<Player> players = new ArrayList<>();
        Player playerToUpdate1 = new Player("Player1");
        playerToUpdate1.setCheckedStatus(new CheckedStatus());
        playerToUpdate1.setId(1);
        Player playerToUpdate2 = new Player("Player1");
        playerToUpdate2.setCheckedStatus(new CheckedStatus());
        playerToUpdate2.setId(2);
        players.add(playerToUpdate1);
        players.add(playerToUpdate2);
        when(playerRepository.findAll()).thenReturn(players);

        PlayerMatchPerformanceStats mockPerformanceStats = mock(PlayerMatchPerformanceStats.class);
        when(parsingService.parsePlayerMatchData(playerToUpdate1)).thenReturn(mockPerformanceStats);
        when(parsingService.parsePlayerMatchData(playerToUpdate2)).thenReturn(mockPerformanceStats);

        doNothing().when(imageGeneratorService).generatePlayerStatImage(any(Post.class));
        doNothing().when(amazonS3Service).uploadToS3(any(Post.class));

        Match match = new Match("https://url", Date.from(Instant.now()), "homeTeamName", "awayTeamName", "relevantTeamName");
        when(mockPerformanceStats.getMatch()).thenReturn(match);

        when(emailService.sendEmailUpdate(anyList())).thenReturn(true);

        when(postRepository.save(any(Post.class))).thenReturn(new Post());

        when(playerRepository.saveAll(any())).thenReturn(List.of(playerToUpdate1));
        UpdatePlayersResponse response = playerService.updateDataForAllPlayers();

        assertTrue(response.isEmailSent());
        assertEquals(2, response.getNumPlayersUpdated());
        assertNotNull(response.getPlayersUpdated());
        assertEquals(2, response.getPlayersUpdated().size());
        assertEquals(playerToUpdate1, response.getPlayersUpdated().get(0));
        assertEquals(playerToUpdate2, response.getPlayersUpdated().get(1));

        // Image gen and upload should be called for each player
        verify(imageGeneratorService, times(2)).generatePlayerStatImage(any(Post.class));
        verify(amazonS3Service, times(2)).uploadToS3(any(Post.class));
    }

    @Test
    public void updateDataForAllPlayers_playersFound_imageGenIsDisabled_successful() throws Exception {
        when(imageGeneratorProperties.isEnabled()).thenReturn(false);

        List<Player> players = new ArrayList<>();
        Player playerToUpdate1 = new Player("Player1");
        playerToUpdate1.setCheckedStatus(new CheckedStatus());
        playerToUpdate1.setId(1);
        Player playerToUpdate2 = new Player("Player1");
        playerToUpdate2.setCheckedStatus(new CheckedStatus());
        playerToUpdate2.setId(2);
        players.add(playerToUpdate1);
        players.add(playerToUpdate2);
        when(playerRepository.findAll()).thenReturn(players);

        PlayerMatchPerformanceStats mockPerformanceStats = mock(PlayerMatchPerformanceStats.class);
        when(parsingService.parsePlayerMatchData(playerToUpdate1)).thenReturn(mockPerformanceStats);
        when(parsingService.parsePlayerMatchData(playerToUpdate2)).thenReturn(mockPerformanceStats);

        Match match = new Match("https://url", Date.from(Instant.now()), "homeTeamName", "awayTeamName", "relevantTeamName");
        when(mockPerformanceStats.getMatch()).thenReturn(match);

        when(emailService.sendEmailUpdate(anyList())).thenReturn(true);

        when(postRepository.save(any(Post.class))).thenReturn(new Post());

        when(playerRepository.saveAll(any())).thenReturn(List.of(playerToUpdate1));
        UpdatePlayersResponse response = playerService.updateDataForAllPlayers();

        assertTrue(response.isEmailSent());
        assertEquals(2, response.getNumPlayersUpdated());
        assertNotNull(response.getPlayersUpdated());
        assertEquals(2, response.getPlayersUpdated().size());
        assertEquals(playerToUpdate1, response.getPlayersUpdated().get(0));
        assertEquals(playerToUpdate2, response.getPlayersUpdated().get(1));

        // Should not call image gen
        verify(imageGeneratorService, times(0)).generatePlayerStatImage(any(Post.class));
        verify(amazonS3Service, times(0)).uploadToS3(any(Post.class));
    }

    @Test
    public void updateDataForPlayers_multiplePlayers_successful() throws Exception {
        List<Player> players = new ArrayList<>();
        Player playerToUpdate1 = new Player("Player1");
        playerToUpdate1.setCheckedStatus(new CheckedStatus());
        playerToUpdate1.setId(1);
        Player playerToUpdate2 = new Player("Player1");
        playerToUpdate2.setCheckedStatus(new CheckedStatus());
        playerToUpdate2.setId(2);
        players.add(playerToUpdate1);
        players.add(playerToUpdate2);

        PlayerMatchPerformanceStats mockPerformanceStats = mock(PlayerMatchPerformanceStats.class);
        when(parsingService.parsePlayerMatchData(playerToUpdate1)).thenReturn(mockPerformanceStats);
        when(parsingService.parsePlayerMatchData(playerToUpdate2)).thenReturn(mockPerformanceStats);

        doNothing().when(imageGeneratorService).generatePlayerStatImage(any(Post.class));
        doNothing().when(amazonS3Service).uploadToS3(any(Post.class));

        Match match = new Match("https://url", Date.from(Instant.now()), "homeTeamName", "awayTeamName", "relevantTeamName");
        when(mockPerformanceStats.getMatch()).thenReturn(match);

        when(emailService.sendEmailUpdate(anyList())).thenReturn(true);

        when(postRepository.save(any(Post.class))).thenReturn(new Post());

        when(playerRepository.saveAll(any())).thenReturn(List.of(playerToUpdate1));
        UpdatePlayersResponse response = playerService.updateDataForPlayers(players);

        assertTrue(response.isEmailSent());
        assertEquals(2, response.getNumPlayersUpdated());
        assertNotNull(response.getPlayersUpdated());
        assertEquals(2, response.getPlayersUpdated().size());
        assertEquals(playerToUpdate1, response.getPlayersUpdated().get(0));
        assertEquals(playerToUpdate2, response.getPlayersUpdated().get(1));
    }

    @Test
    public void updateDataForPlayers_noPlayersToUpdate_returnEmptyResponse() throws InterruptedException {
        List<Player> noPlayers = new ArrayList<>();
        UpdatePlayersResponse response = playerService.updateDataForPlayers(noPlayers);

        assertFalse(response.isEmailSent());
        assertEquals(0, response.getNumPlayersUpdated());
        assertNull(response.getPlayersUpdated());
    }

    @Test
    public void updateDataForPlayers_onePlayer_parsingFailed_noPlayersUpdated() throws InterruptedException {
        List<Player> players = new ArrayList<>();
        Integer playerId = 1;
        Player playerToUpdate = new Player("Player1");
        playerToUpdate.setCheckedStatus(new CheckedStatus());
        playerToUpdate.setId(playerId);
        players.add(playerToUpdate);

        when(parsingService.parsePlayerMatchData(playerToUpdate)).thenReturn(null);

        UpdatePlayersResponse response = playerService.updateDataForPlayers(players);

        assertFalse(response.isEmailSent());
        assertEquals(0, response.getNumPlayersUpdated());
        assertNull(response.getPlayersUpdated());
    }

    @Test
    public void updateDataForPlayers_emailSendingFailed_continueUpdatingPlayers() throws Exception {
        List<Player> players = new ArrayList<>();
        Integer playerId = 1;
        Player playerToUpdate = new Player("Player1");
        playerToUpdate.setCheckedStatus(new CheckedStatus());
        playerToUpdate.setId(playerId);
        players.add(playerToUpdate);

        PlayerMatchPerformanceStats mockPerformanceStats = mock(PlayerMatchPerformanceStats.class);
        when(parsingService.parsePlayerMatchData(playerToUpdate)).thenReturn(mockPerformanceStats);

        doNothing().when(imageGeneratorService).generatePlayerStatImage(any(Post.class));
        doNothing().when(amazonS3Service).uploadToS3(any(Post.class));

        Match match = new Match("https://url", Date.from(Instant.now()), "homeTeamName", "awayTeamName", "relevantTeamName");
        when(mockPerformanceStats.getMatch()).thenReturn(match);

        when(emailService.sendEmailUpdate(anyList())).thenReturn(false);

        when(postRepository.save(any(Post.class))).thenReturn(new Post());

        when(playerRepository.saveAll(any())).thenReturn(List.of(playerToUpdate));

        UpdatePlayersResponse response = playerService.updateDataForPlayers(players);

        assertFalse(response.isEmailSent());
        assertEquals(1, response.getNumPlayersUpdated());
        assertNotNull(response.getPlayersUpdated());
        assertEquals(1, response.getPlayersUpdated().size());
        assertEquals(playerToUpdate, response.getPlayersUpdated().get(0));
    }

    @Test
    public void updateDataForPlayers_onePlayer_imageGenerationException_noPlayersUpdated() throws Exception {
        List<Player> players = new ArrayList<>();
        Integer playerId = 1;
        Player playerToUpdate = new Player("Player1");
        playerToUpdate.setCheckedStatus(new CheckedStatus());
        playerToUpdate.setId(playerId);
        players.add(playerToUpdate);

        PlayerMatchPerformanceStats mockPerformanceStats = mock(PlayerMatchPerformanceStats.class);
        when(parsingService.parsePlayerMatchData(playerToUpdate)).thenReturn(mockPerformanceStats);

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(playerToUpdate));

        doThrow(new Exception("Error while generating stat image")).when(imageGeneratorService).generatePlayerStatImage(any(Post.class));

        UpdatePlayersResponse response = playerService.updateDataForPlayers(players);

        assertFalse(response.isEmailSent());
        assertEquals(0, response.getNumPlayersUpdated());
        assertNull(response.getPlayersUpdated());
    }

    @Test
    public void updateDataForPlayers_onePlayer_uploadToS3Failed_noPlayersUpdated() throws Exception {
        List<Player> players = new ArrayList<>();
        Integer playerId = 1;
        Player playerToUpdate = new Player("Player1");
        playerToUpdate.setCheckedStatus(new CheckedStatus());
        playerToUpdate.setId(playerId);
        players.add(playerToUpdate);

        PlayerMatchPerformanceStats mockPerformanceStats = mock(PlayerMatchPerformanceStats.class);
        when(parsingService.parsePlayerMatchData(playerToUpdate)).thenReturn(mockPerformanceStats);

        doNothing().when(imageGeneratorService).generatePlayerStatImage(any(Post.class));
        doThrow(new Exception("Error while generating stat image")).when(amazonS3Service).uploadToS3(any(Post.class));

        UpdatePlayersResponse response = playerService.updateDataForPlayers(players);

        assertFalse(response.isEmailSent());
        assertEquals(0, response.getNumPlayersUpdated());
        assertNull(response.getPlayersUpdated());
    }

    @Test
    public void updateDataForPlayers_multiplePlayers_parsingForAllFailed_noPlayersUpdated() throws InterruptedException {
        // Both players fail to parse
        List<Player> players = new ArrayList<>();
        Player playerToUpdate1 = new Player("Player1");
        playerToUpdate1.setCheckedStatus(new CheckedStatus());
        playerToUpdate1.setId(1);
        Player playerToUpdate2 = new Player("Player1");
        playerToUpdate2.setCheckedStatus(new CheckedStatus());
        playerToUpdate2.setId(2);
        players.add(playerToUpdate1);
        players.add(playerToUpdate2);

        when(parsingService.parsePlayerMatchData(playerToUpdate1)).thenReturn(null);
        when(parsingService.parsePlayerMatchData(playerToUpdate2)).thenReturn(null);

        UpdatePlayersResponse response = playerService.updateDataForPlayers(players);

        assertFalse(response.isEmailSent());
        assertEquals(0, response.getNumPlayersUpdated());
        assertNull(response.getPlayersUpdated());
    }

    @Test
    public void updateDataForPlayers_multiplePlayers_imageGenerationException_onePlayerUpdated() throws Exception {
        // First player has imageGeneration error; second player is successful
        // Should only have updated second player, and sent in response
        List<Player> players = new ArrayList<>();
        Player playerToUpdate1 = new Player("Player1");
        playerToUpdate1.setCheckedStatus(new CheckedStatus());
        playerToUpdate1.setId(1);
        Player playerToUpdate2 = new Player("Player1");
        playerToUpdate2.setCheckedStatus(new CheckedStatus());
        playerToUpdate2.setId(2);
        players.add(playerToUpdate1);
        players.add(playerToUpdate2);

        PlayerMatchPerformanceStats mockPerformanceStats = mock(PlayerMatchPerformanceStats.class);
        when(parsingService.parsePlayerMatchData(playerToUpdate1)).thenReturn(mockPerformanceStats);
        when(parsingService.parsePlayerMatchData(playerToUpdate2)).thenReturn(mockPerformanceStats);

        // First player error
        // Second player success
        doThrow(new Exception("Error while generating stat image")).doNothing().when(imageGeneratorService).generatePlayerStatImage(any(Post.class));
        doNothing().when(amazonS3Service).uploadToS3(any(Post.class));

        Match match = new Match("https://url", Date.from(Instant.now()), "homeTeamName", "awayTeamName", "relevantTeamName");
        when(mockPerformanceStats.getMatch()).thenReturn(match);

        when(emailService.sendEmailUpdate(anyList())).thenReturn(true);

        when(postRepository.save(any(Post.class))).thenReturn(new Post());

        when(playerRepository.saveAll(any())).thenReturn(List.of(playerToUpdate1));
        UpdatePlayersResponse response = playerService.updateDataForPlayers(players);

        assertTrue(response.isEmailSent());
        assertEquals(1, response.getNumPlayersUpdated());
        assertNotNull(response.getPlayersUpdated());
        assertEquals(1, response.getPlayersUpdated().size());
        assertEquals(playerToUpdate2, response.getPlayersUpdated().get(0));
    }

    @Test
    public void updateDataForPlayers_multiplePlayers_uploadToS3Failed_onePlayerUpdated() throws Exception {
        // First player has uploadToS3 error; second player is successful
        // Should only have updated second player, and sent in response
        List<Player> players = new ArrayList<>();
        Player playerToUpdate1 = new Player("Player1");
        playerToUpdate1.setCheckedStatus(new CheckedStatus());
        playerToUpdate1.setId(1);
        Player playerToUpdate2 = new Player("Player1");
        playerToUpdate2.setCheckedStatus(new CheckedStatus());
        playerToUpdate2.setId(2);
        players.add(playerToUpdate1);
        players.add(playerToUpdate2);

        PlayerMatchPerformanceStats mockPerformanceStats = mock(PlayerMatchPerformanceStats.class);
        when(parsingService.parsePlayerMatchData(playerToUpdate1)).thenReturn(mockPerformanceStats);
        when(parsingService.parsePlayerMatchData(playerToUpdate2)).thenReturn(mockPerformanceStats);

        // First player error
        // Second player success
        doNothing().when(imageGeneratorService).generatePlayerStatImage(any(Post.class));
        doThrow(new Exception("Error while generating stat image")).doNothing().when(amazonS3Service).uploadToS3(any(Post.class));

        Match match = new Match("https://url", Date.from(Instant.now()), "homeTeamName", "awayTeamName", "relevantTeamName");
        when(mockPerformanceStats.getMatch()).thenReturn(match);

        when(emailService.sendEmailUpdate(anyList())).thenReturn(true);

        when(postRepository.save(any(Post.class))).thenReturn(new Post());

        when(playerRepository.saveAll(any())).thenReturn(List.of(playerToUpdate1));
        UpdatePlayersResponse response = playerService.updateDataForPlayers(players);

        assertTrue(response.isEmailSent());
        assertEquals(1, response.getNumPlayersUpdated());
        assertNotNull(response.getPlayersUpdated());
        assertEquals(1, response.getPlayersUpdated().size());
        assertEquals(playerToUpdate2, response.getPlayersUpdated().get(0));
    }
}
