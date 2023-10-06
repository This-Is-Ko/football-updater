package com.ko.footballupdater.models;

import lombok.Getter;

import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
public class Match {

    private final String url;
    private final Date date;
    private final String homeTeamName;
    private final String awayTeamName;
    private final String relevantTeam;

    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
    private final SimpleDateFormat formatterFileName = new SimpleDateFormat("yyyy_MM_dd");

    public Match(String url, Date date, String homeTeamName, String awayTeamName, String relevantTeam) {
        this.url = url;
        this.date = date;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
        this.relevantTeam = relevantTeam;
    }

    public String getDateAsFormattedString() {
        return formatter.format(date);
    }

    public String getDateAsFormattedStringForFileName() {
        return formatterFileName.format(date);
    }
}
