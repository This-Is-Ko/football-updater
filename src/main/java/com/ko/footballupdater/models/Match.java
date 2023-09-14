package com.ko.footballupdater.models;

public class Match {

    private final String url;
    private final String homeTeamName;
    private final String awayTeamName;
    private final String relevantTeam;

    public Match(String url, String homeTeamName, String awayTeamName, String relevantTeam) {
        this.url = url;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
        this.relevantTeam = relevantTeam;
    }

    public String getUrl() {
        return url;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }

    public String getRelevantTeam() {
        return relevantTeam;
    }
}
