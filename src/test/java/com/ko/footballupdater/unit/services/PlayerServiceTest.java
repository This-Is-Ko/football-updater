package com.ko.footballupdater.unit.services;



import com.amazonaws.services.kms.model.NotFoundException;
import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;
import com.ko.footballupdater.repositories.PlayerRepository;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.repositories.TeamRepository;
import com.ko.footballupdater.responses.AddNewTeamResponse;
import com.ko.footballupdater.responses.UpdatePlayersResponse;
import com.ko.footballupdater.services.AmazonS3Service;
import com.ko.footballupdater.services.EmailService;
import com.ko.footballupdater.services.ImageGeneratorService;
import com.ko.footballupdater.services.ParsingService;
import com.ko.footballupdater.services.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
    private ImageGeneratorService imageGeneratorService;

    @Mock
    private InstagramPostProperies instagramPostProperies;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(instagramPostProperies.getVersion()).thenReturn(2);
        when(instagramPostProperies.getDefaultHashtags()).thenReturn("#default");
        when(instagramPostProperies.getAccountName()).thenReturn("Insta account name");
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
            assert e.getMessage().equals("Player already exists");
        }
    }

    @Test
    public void updateDataForPlayer_validPlayer_successful() throws Exception {
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
            assert e.getErrorMessage().equals("Player name not found");
        }
    }

    @Test
    public void updateDataForPlayers_noPlayersToUpdate_returnEmptyResponse() {
        List<Player> noPlayers = new ArrayList<>();
        UpdatePlayersResponse response = playerService.updateDataForPlayers(noPlayers);

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
}
