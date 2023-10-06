package com.ko.footballupdater.models;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class InstagramPost {

    private Player player;
    private PlayerMatchPerformanceStats playerMatchPerformanceStats;
    private List<String> imagesFileNames = new ArrayList<>();
    private List<String> imagesS3Urls = new ArrayList<>();

    public InstagramPost(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        this.player = player;
        this.playerMatchPerformanceStats = playerMatchPerformanceStats;
    }
}
