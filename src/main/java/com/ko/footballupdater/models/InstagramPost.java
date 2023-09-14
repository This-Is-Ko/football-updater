package com.ko.footballupdater.models;

public class InstagramPost {

    private Player player;
    private PlayerMatchPerformanceStats playerMatchPerformanceStats;
    private String caption;
    private String imageSearchUrl;

    public InstagramPost(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        this.player = player;
        this.playerMatchPerformanceStats = playerMatchPerformanceStats;
        this.caption = generateDefaultPlayerCaption(player, playerMatchPerformanceStats);
        this.imageSearchUrl = generateImageSearchUrl(player, playerMatchPerformanceStats);
    }

    public String generateDefaultPlayerCaption(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        return String.format("%s stats in %s vs %s\n",
                player.getName(),
                playerMatchPerformanceStats.getMatch().getHomeTeamName(),
                playerMatchPerformanceStats.getMatch().getAwayTeamName()
        ) + playerMatchPerformanceStats.toFormattedString();
    }

    private String generateImageSearchUrl(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        String searchPhrase = player.getName() + " " + playerMatchPerformanceStats.getMatch().getRelevantTeam();
        return String.format("https://www.google.com/search?q=%s&tbm=isch&hl=en&tbs=qdr:w", searchPhrase.replaceAll(" ", "%20"));
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
