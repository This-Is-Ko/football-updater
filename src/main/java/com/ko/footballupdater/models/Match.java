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

    public Match() {
    }

    public Match(String url, Date date, String homeTeamName, String awayTeamName, String relevantTeam) {
        this.url = url;
        this.date = date;
        this.homeTeamName = homeTeamName;
        this.awayTeamName = awayTeamName;
        this.relevantTeam = relevantTeam;
    }
}
