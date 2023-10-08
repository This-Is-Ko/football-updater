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

@Getter
@Setter
@Entity
@Table(name = "checked_statuses")
public class CheckedStatus {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @NotNull
    @Column
    @Enumerated(EnumType.STRING)
    private DataSourceSiteName siteName;

    @NotNull
    @Column
    private Date lastChecked = new Date();

    @Column
    private String latestCheckedMatchUrl;

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
