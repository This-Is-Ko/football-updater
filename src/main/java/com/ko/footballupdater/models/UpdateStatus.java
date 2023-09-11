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
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Entity
@Table(name = "update_statuses")
public class UpdateStatus {

    public UpdateStatus () {
    }

    public UpdateStatus (DataSourceSiteName siteName) {
        this.siteName = siteName;
    }

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
    private String lastestData;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DataSourceSiteName getSiteName() {
        return siteName;
    }

    public void setSiteName(DataSourceSiteName siteName) {
        this.siteName = siteName;
    }

    public Date getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(Date lastChecked) {
        this.lastChecked = lastChecked;
    }

    public String getLastestData() {
        return lastestData;
    }

    public void setLastestData(String lastestData) {
        this.lastestData = lastestData;
    }
}
