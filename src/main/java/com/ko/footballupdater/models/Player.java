package com.ko.footballupdater.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @NotNull
    @Column
    private String name;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @Column
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    private Date dob;

    @OneToMany
    @JoinColumn(name = "image_id")
    @JdbcTypeCode(SqlTypes.JSON)
    private Set<Image> images;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "player_data_source_id")
    @JdbcTypeCode(SqlTypes.JSON)
    private Set<DataSource> dataSources;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "checked_status_id")
    private CheckedStatus checkedStatus;

    public Player() {
    }

    public Player(String name) {
        this.name = name;
    }

    public Player(String name, Date dob) {
        this.name = name;
        this.dob = dob;
    }
}