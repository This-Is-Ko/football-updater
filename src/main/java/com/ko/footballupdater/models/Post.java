package com.ko.footballupdater.models;

import com.ko.footballupdater.converter.StringListConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Integer id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "player_id")
    @JdbcTypeCode(SqlTypes.JSON)
    private Player player;

    @Convert(converter = StringListConverter.class)
    @Column(name = "image_urls")
    private List<String> imagesUrls;

    private String dateAsString;

    @Column(name = "caption")
    private String caption;

    @NotNull
    private boolean postedStatus;

    public Post() {
    }

    public Post(Player player) {
        this.player = player;
        imagesUrls = new ArrayList<>();
    }

    public Post(Integer id, boolean postedStatus) {
        this.id = id;
        this.postedStatus = postedStatus;
        imagesUrls = new ArrayList<>();
    }

    public Post(Player player, String dateAsString, List<String> imagesUrls) {
        this.player = player;
        this.dateAsString = dateAsString;
        this.imagesUrls = imagesUrls;
        this.postedStatus = false;
    }
}
