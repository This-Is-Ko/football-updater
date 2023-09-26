package com.ko.footballupdater.models;

import com.ko.footballupdater.utils.PostHelper;

public class InstagramPost {

    private Player player;
    private PlayerMatchPerformanceStats playerMatchPerformanceStats;
    private String caption;
    private String imageSearchUrl;

    public InstagramPost(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        this.player = player;
        this.playerMatchPerformanceStats = playerMatchPerformanceStats;
        this.caption = PostHelper.generatePostDefaultPlayerCaption(player, playerMatchPerformanceStats);
        this.imageSearchUrl = PostHelper.generatePostImageSearchUrl(player, playerMatchPerformanceStats);
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public PlayerMatchPerformanceStats getPlayerMatchPerformanceStats() {
        return playerMatchPerformanceStats;
    }

    public void setPlayerMatchPerformanceStats(PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        this.playerMatchPerformanceStats = playerMatchPerformanceStats;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getImageSearchUrl() {
        return imageSearchUrl;
    }

    public void setImageSearchUrl(String imageSearchUrl) {
        this.imageSearchUrl = imageSearchUrl;
    }
}
