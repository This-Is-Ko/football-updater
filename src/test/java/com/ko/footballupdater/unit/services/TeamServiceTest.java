package com.ko.footballupdater.unit.services;

import com.amazonaws.services.kms.model.NotFoundException;
import com.ko.footballupdater.models.AlternativeTeamName;
import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.DataSourceType;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.repositories.TeamRepository;
import com.ko.footballupdater.request.AddTeamRequest;
import com.ko.footballupdater.request.RequestDataSource;
import com.ko.footballupdater.request.UpdateTeamRequest;
import com.ko.footballupdater.responses.AddNewTeamResponse;
import com.ko.footballupdater.services.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TeamServiceTest {

    @InjectMocks
    private TeamService teamService;

    private TeamRepository teamRepository = mock(TeamRepository.class);

    private final Integer TEAM_ID = 1234;
    private final String TEAM_NAME = "Team name";

    @BeforeEach
    public void setup() {
        teamRepository = mock(TeamRepository.class);
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void addTeam_addingTeam_successfully() {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Collections.emptyList());
        AddTeamRequest addTeamRequest = new AddTeamRequest();
        addTeamRequest.setName(TEAM_NAME);
        addTeamRequest.setAdditionalHashtags(new ArrayList<>(Arrays.asList("#hashtag0", "#hashtag1")));
        addTeamRequest.setAlternativeNames(new ArrayList<>(Arrays.asList("alt name0", "alt name1")));
        addTeamRequest.setDataSources(Arrays.asList(
                new RequestDataSource(DataSourceSiteName.FOTMOB, "http://test0.com"),
                new RequestDataSource(DataSourceSiteName.FBREF, "http://test1.com")));
        addTeamRequest.setPopulatePlayers(false);
        AddNewTeamResponse response = new AddNewTeamResponse();

        Team addedTeam = new Team(TEAM_ID, TEAM_NAME);
        when(teamRepository.save(any(Team.class))).thenReturn(addedTeam);

        teamService.addTeam(addTeamRequest, response);

        assertEquals(TEAM_NAME, response.getTeam().getName());
        assertEquals(TEAM_ID, response.getTeam().getId());
    }

    @Test
    public void addTeam_addingTeamWithNoHashtags_successfully() {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Collections.emptyList());
        AddTeamRequest addTeamRequest = new AddTeamRequest();
        addTeamRequest.setName(TEAM_NAME);
        addTeamRequest.setAlternativeNames(new ArrayList<>(Arrays.asList("alt name0", "alt name1")));
        addTeamRequest.setDataSources(Arrays.asList(
                new RequestDataSource(DataSourceSiteName.FOTMOB, "http://test0.com"),
                new RequestDataSource(DataSourceSiteName.FBREF, "http://test1.com")));
        addTeamRequest.setPopulatePlayers(false);
        AddNewTeamResponse response = new AddNewTeamResponse();

        Team addedTeam = new Team(TEAM_ID, TEAM_NAME);
        when(teamRepository.save(any(Team.class))).thenReturn(addedTeam);

        teamService.addTeam(addTeamRequest, response);

        assertEquals(TEAM_NAME, response.getTeam().getName());
        assertEquals(TEAM_ID, response.getTeam().getId());
    }

    @Test
    public void addTeam_addingTeamWithNoAltNames_successfully() {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Collections.emptyList());
        AddTeamRequest addTeamRequest = new AddTeamRequest();
        addTeamRequest.setName(TEAM_NAME);
        addTeamRequest.setAdditionalHashtags(new ArrayList<>(Arrays.asList("#hashtag0", "#hashtag1")));
        addTeamRequest.setDataSources(Arrays.asList(
                new RequestDataSource(DataSourceSiteName.FOTMOB, "http://test0.com"),
                new RequestDataSource(DataSourceSiteName.FBREF, "http://test1.com")));
        addTeamRequest.setPopulatePlayers(false);
        AddNewTeamResponse response = new AddNewTeamResponse();

        Team addedTeam = new Team(TEAM_ID, TEAM_NAME);
        when(teamRepository.save(any(Team.class))).thenReturn(addedTeam);

        teamService.addTeam(addTeamRequest, response);

        assertEquals(TEAM_NAME, response.getTeam().getName());
        assertEquals(TEAM_ID, response.getTeam().getId());
    }

    @Test
    public void addTeam_addingTeamWithNoDataSources_successfully() {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Collections.emptyList());
        AddTeamRequest addTeamRequest = new AddTeamRequest();
        addTeamRequest.setName(TEAM_NAME);
        addTeamRequest.setAdditionalHashtags(new ArrayList<>(Arrays.asList("#hashtag0", "#hashtag1")));
        addTeamRequest.setAlternativeNames(new ArrayList<>(Arrays.asList("alt name0", "alt name1")));
        addTeamRequest.setPopulatePlayers(false);
        AddNewTeamResponse response = new AddNewTeamResponse();

        Team addedTeam = new Team(TEAM_ID, TEAM_NAME);
        when(teamRepository.save(any(Team.class))).thenReturn(addedTeam);

        teamService.addTeam(addTeamRequest, response);

        assertEquals(TEAM_NAME, response.getTeam().getName());
        assertEquals(TEAM_ID, response.getTeam().getId());
    }

    @Test
    public void addTeam_addingTeamWithNoPopulatePlayersBooleanSet_successfully() {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(Collections.emptyList());
        AddTeamRequest addTeamRequest = new AddTeamRequest();
        addTeamRequest.setName(TEAM_NAME);
        AddNewTeamResponse response = new AddNewTeamResponse();

        Team addedTeam = new Team(TEAM_ID, TEAM_NAME);
        when(teamRepository.save(any(Team.class))).thenReturn(addedTeam);

        teamService.addTeam(addTeamRequest, response);

        assertEquals(TEAM_NAME, response.getTeam().getName());
        assertEquals(TEAM_ID, response.getTeam().getId());
    }

    @Test
    public void addTeam_teamAlreadyExists() {
        when(teamRepository.findByName(TEAM_NAME)).thenReturn(List.of(new Team()));
        AddTeamRequest addTeamRequest = new AddTeamRequest();
        addTeamRequest.setName(TEAM_NAME);
        addTeamRequest.setAdditionalHashtags(new ArrayList<>(List.of("#hashtag")));
        addTeamRequest.setAlternativeNames(new ArrayList<>(List.of("alt name")));
        addTeamRequest.setPopulatePlayers(false);
        AddNewTeamResponse response = new AddNewTeamResponse();
        try {
            teamService.addTeam(addTeamRequest, response);
        } catch (IllegalArgumentException e) {
            assertEquals("Team already exists", e.getMessage());
        }
    }

    @Test
    public void updateTeam_updateWithValidInput_successfully() {
        Team existingTeam = new Team(TEAM_ID, TEAM_NAME);
        Set<DataSource> existingDataSources = new HashSet<>(Arrays.asList(
                new DataSource(DataSourceType.TEAM, DataSourceSiteName.FOTMOB, "http://site1.com"),
                new DataSource(DataSourceType.TEAM, DataSourceSiteName.FBREF, "http://site2.com")));
        Set<AlternativeTeamName> existingAlternativeTeamNames = new HashSet<>(Arrays.asList(
                new AlternativeTeamName("AltName1"), new AlternativeTeamName("AltName2")));

        existingTeam.setDataSources(existingDataSources);
        existingTeam.setAlternativeTeamNames(existingAlternativeTeamNames);

        UpdateTeamRequest updateTeamRequest = new UpdateTeamRequest();
        updateTeamRequest.setDataSources(List.of(
                new RequestDataSource(DataSourceSiteName.SOFASCORE, "http://test0.com")));
        updateTeamRequest.setAlternativeNames(new ArrayList<>(Arrays.asList("NewAltName1", "NewAltName2")));
        updateTeamRequest.setAdditionalHashtags(new ArrayList<>(Arrays.asList("#hashtag0", "#hashtag1")));

        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(existingTeam));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        teamService.updateTeam(TEAM_ID, updateTeamRequest, new AddNewTeamResponse());

        assertEquals(TEAM_NAME, existingTeam.getName());
        assertEquals(TEAM_ID, existingTeam.getId());
        assertEquals(3, existingTeam.getDataSources().size());
        assertEquals(4, existingTeam.getAlternativeTeamNames().size());
    }

    @Test
    public void updateTeam_teamNotFound_throwNotFoundException() {
        UpdateTeamRequest updateTeamRequest = new UpdateTeamRequest();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.empty());

        try {
            teamService.updateTeam(TEAM_ID, updateTeamRequest, new AddNewTeamResponse());
        } catch (NotFoundException e) {
            assertEquals("Team can't be found", e.getErrorMessage());
        }
    }

    @Test
    public void updateTeam_invalidHashtag_missingHash_throwException() {
        UpdateTeamRequest updateTeamRequest = new UpdateTeamRequest();
        updateTeamRequest.setAdditionalHashtags(new ArrayList<>(Arrays.asList("#hashtag0", "invalid", "#hashtag1")));

        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team(TEAM_ID, TEAM_NAME)));

        try {
            teamService.updateTeam(TEAM_ID, updateTeamRequest, new AddNewTeamResponse());
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid hashtag value", e.getMessage());
        }
    }

    @Test
    public void updateTeam_invalidHashtag_spaceInHashtag_throwException() {
        UpdateTeamRequest updateTeamRequest = new UpdateTeamRequest();
        updateTeamRequest.setAdditionalHashtags(new ArrayList<>(Arrays.asList("#hashtag0", "#invalid hashtag", "#hashtag1")));

        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team(TEAM_ID, TEAM_NAME)));

        try {
            teamService.updateTeam(TEAM_ID, updateTeamRequest, new AddNewTeamResponse());
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid hashtag value", e.getMessage());
        }
    }

    @Test
    public void updateTeam_invalidHashtag_tabInHashtag_throwException() {
        UpdateTeamRequest updateTeamRequest = new UpdateTeamRequest();
        updateTeamRequest.setAdditionalHashtags(new ArrayList<>(Arrays.asList("#hashtag0", "#invalid\thashtag", "#hashtag1")));

        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(new Team(TEAM_ID, TEAM_NAME)));

        try {
            teamService.updateTeam(TEAM_ID, updateTeamRequest, new AddNewTeamResponse());
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid hashtag value", e.getMessage());
        }
    }

    @Test
    public void deleteTeam_deletesTeam_Successful() {
        Team teamToDelete = new Team(TEAM_ID, "Team 1");
        when(teamRepository.existsById(teamToDelete.getId())).thenReturn(true);
        doNothing().when(teamRepository).deleteById(TEAM_ID);
        teamService.deleteTeam(teamToDelete.getId());
        verify(teamRepository).deleteById(TEAM_ID);
    }

    @Test
    public void deleteTeam_invalidTeamId() {
        when(teamRepository.existsById(TEAM_ID)).thenReturn(false);
        try {
            teamService.deleteTeam(TEAM_ID);
        } catch (IllegalArgumentException e) {
            assertEquals("Team doesn't exist", e.getMessage());
        }
    }

}
