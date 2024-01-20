package com.ko.footballupdater.controllers;


import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.request.AddTeamRequest;
import com.ko.footballupdater.request.UpdateTeamRequest;
import com.ko.footballupdater.responses.AddNewTeamResponse;
import com.ko.footballupdater.services.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping(path="/api/team")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @PostMapping(path="/")
    public @ResponseBody ResponseEntity<AddNewTeamResponse> addNewTeam(@RequestBody AddTeamRequest addTeamRequest) {
        try {
            AddNewTeamResponse response = new AddNewTeamResponse();
            teamService.addTeam(addTeamRequest, response);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Adding new team failed", ex);
        }
    }

    @PatchMapping(path="/{teamId}")
    public @ResponseBody ResponseEntity<AddNewTeamResponse> updateTeam(
            @PathVariable("teamId") Integer teamId,
            @RequestBody UpdateTeamRequest updateTeamRequest) {
        try {
            AddNewTeamResponse response = new AddNewTeamResponse();
            teamService.updateTeam(teamId, updateTeamRequest, response);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Updating team failed", ex);
        }
    }

    @DeleteMapping("/{teamId}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteTeam(@PathVariable("teamId") Integer teamId) {
        try {
            teamService.deleteTeam(teamId);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Error while attempting to delete team", ex);
        }
    }

    @GetMapping(path="/all")
    public @ResponseBody Iterable<Team> getAllTeams() {
        return teamService.getTeams();
    }
}