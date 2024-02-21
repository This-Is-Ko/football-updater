package com.ko.footballupdater.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Represents the status of checking data for a specific player/team.
 */
@Getter
@Setter
@Entity
@Table(name = "checked_statuses")
public class CheckedStatus {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    /**
     * The data source that was checked.
     */
    @NotNull
    @Column
    @Enumerated(EnumType.STRING)
    private DataSourceSiteName siteName;

    /**
     * The date when the data was last checked.
     */
    @NotNull
    @Column
    private Date lastChecked = new Date();

    /**
     * The URL of the latest checked match.
     */
    @Column
    private String latestCheckedMatchUrl;

    /**
     * The date of the latest checked match.
     */
    @Column
    private Date latestCheckedMatchDate;

    public CheckedStatus() {
    }

    public CheckedStatus(DataSourceSiteName siteName) {
        this.siteName = siteName;
    }

    public CheckedStatus(Integer id, DataSourceSiteName siteName) {
        this.id = id;
        this.siteName = siteName;
    }
}
