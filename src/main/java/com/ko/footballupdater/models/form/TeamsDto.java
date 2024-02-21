package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Team;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TeamsDto {
    private List<Team> teams;
}
