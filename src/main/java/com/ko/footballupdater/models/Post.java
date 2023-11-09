package com.ko.footballupdater.models;

import com.ko.footballupdater.converter.StringListConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    // Any null can be assumed as standard ALL_STAT_POST
    @Column
    @Enumerated(EnumType.STRING)
    private PostType postType;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "player_id")
    @JdbcTypeCode(SqlTypes.JSON)
    private Player player;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "match_performance_stats_id")
    @JdbcTypeCode(SqlTypes.JSON)
    private PlayerMatchPerformanceStats playerMatchPerformanceStats;

    @Convert(converter = StringListConverter.class)
    @Column(name = "image_urls")
    private List<String> imagesUrls;

    @Transient
    @Convert(converter = StringListConverter.class)
    private List<String> imageSearchUrls = new ArrayList<>();

    @Transient
    @Convert(converter = StringListConverter.class)
    private List<String> imagesFileNames = new ArrayList<>();

    @NotNull
    private Date dateGenerated = new Date();

    @Column(name = "caption")
    private String caption;

    @NotNull
    private boolean postedStatus;

    public Post() {
    }

    public Post(PostType postType, Player player) {
        this.postType = postType;
        this.player = player;
    }

    public Post(PostType postType, Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        this.postType = postType;
        this.player = player;
        this.playerMatchPerformanceStats = playerMatchPerformanceStats;
        imagesUrls = new ArrayList<>();
    }

    public Post(Integer id, boolean postedStatus) {
        this.id = id;
        this.postedStatus = postedStatus;
        imagesUrls = new ArrayList<>();
    }

    public Post(Player player, List<String> imagesUrls) {
        this.player = player;
        this.imagesUrls = imagesUrls;
        this.postedStatus = false;
    }
}
