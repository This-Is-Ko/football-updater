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

@Getter
@Entity
@Table(name = "data_sources")
public class DataSource {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @NotNull
    @Column
    @Enumerated(EnumType.STRING)
    private DataSourceType type;

    @NotNull
    @Column
    @Enumerated(EnumType.STRING)
    private DataSourceSiteName siteName;

    @NotNull
    @Column
    private String url;

    public DataSource() {
    }

    public DataSource(DataSourceType type, DataSourceSiteName siteName, String url) {
        this.type = type;
        this.siteName = siteName;
        this.url = url;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setType(DataSourceType type) {
        this.type = type;
    }

    public void setSiteName(DataSourceSiteName siteName) {
        this.siteName = siteName;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
