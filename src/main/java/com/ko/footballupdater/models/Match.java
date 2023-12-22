package com.ko.footballupdater.models;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "match_data")
public class Match {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @Nullable
    @Column
    private String url;

    @Nullable
    @Column
    private Date date;

    @Nullable
    @Column
    private String homeTeamName;

    @Nullable
    @Column
    private String awayTeamName;

    @Nullable
    @Column
    private String relevantTeam;

    @Nullable
    @Column
    private Integer homeTeamScore;

    @Nullable
    @Column
    private Integer awayTeamScore;

    public Match() {
    }

    public Match(@Nullable String url, @Nullable Date date, @Nullable String homeTeamName, @Nullable String awayTeamName, @Nullable String relevantTeam) {
        this.url = url;
        this.date = date;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
        this.relevantTeam = relevantTeam;
    }

    public Match(@Nullable String url, @Nullable Date date, @Nullable String homeTeamName, @Nullable String awayTeamName, @Nullable String relevantTeam, @Nullable Integer homeTeamScore, @Nullable Integer awayTeamScore) {
        this.url = url;
        this.date = date;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
        this.relevantTeam = relevantTeam;
        this.homeTeamScore = homeTeamScore;
        this.awayTeamScore = awayTeamScore;
    }
}
