package com.ko.footballupdater.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Set;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @Column
    @NotNull
    private String name;

    @Column
    @NotNull
    private String country;

    @Column
    private String league;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "team_data_source_id")
    @JdbcTypeCode(SqlTypes.JSON)
    private Set<DataSource> dataSources;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "checked_status_id")
    private CheckedStatus checkedStatus;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getLeague() {
        return league;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public Set<DataSource> getDataSources() {
        return dataSources;
    }

    public void setDataSources(Set<DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    public CheckedStatus getCheckedStatus() {
        return checkedStatus;
    }

    public void setCheckedStatus(CheckedStatus checkedStatus) {
        this.checkedStatus = checkedStatus;
    }
}
