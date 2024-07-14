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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @NotNull
    @Column
    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "team_data_source_id")
    @JdbcTypeCode(SqlTypes.JSON)
    private Set<DataSource> dataSources;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "checked_status_id")
    private CheckedStatus checkedStatus;

    @JoinColumn(name = "alternative_team_names_id")
    @OneToMany(cascade = CascadeType.ALL)
    private Set<AlternativeTeamName> alternativeTeamNames;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Hashtag> additionalHashtags;

    @NotNull
    @Column
    private String logoFileName;

    public Team() {
    }

    public Team(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}
