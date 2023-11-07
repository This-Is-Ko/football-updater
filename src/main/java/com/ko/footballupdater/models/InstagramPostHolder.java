package com.ko.footballupdater.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class InstagramPostHolder {

    private Post post;
    private PlayerMatchPerformanceStats playerMatchPerformanceStats;
    private List<String> imagesFileNames = new ArrayList<>();

    public InstagramPostHolder(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        this.post = new Post(player);
        this.playerMatchPerformanceStats = playerMatchPerformanceStats;
    }

}
