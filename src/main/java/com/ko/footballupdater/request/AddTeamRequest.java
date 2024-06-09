package com.ko.footballupdater.request;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AddTeamRequest {

    @NotNull
    private String name;
    private List<RequestDataSource> dataSources;
    private ArrayList<String> alternativeNames;
    private ArrayList<String> additionalHashtags;
    private Boolean populatePlayers;
    private String logoFileName;

}
