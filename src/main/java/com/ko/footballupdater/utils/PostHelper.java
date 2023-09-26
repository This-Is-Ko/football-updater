package com.ko.footballupdater.utils;

import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;

public class PostHelper {

    public static String generatePostDefaultPlayerCaption(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        return String.format("%s stats in %s vs %s on %s\n",
                player.getName(),
                playerMatchPerformanceStats.getMatch().getHomeTeamName(),
                playerMatchPerformanceStats.getMatch().getAwayTeamName(),
                playerMatchPerformanceStats.getMatch().getDateAsFormattedString()
        ) + playerMatchPerformanceStats.toFormattedString() + generatePlayerHashtags(player, playerMatchPerformanceStats);
    }

    public static String generatePostImageSearchUrl(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        String searchPhrase = player.getName() + " " + playerMatchPerformanceStats.getMatch().getRelevantTeam();
        return String.format("https://www.google.com/search?q=%s&tbm=isch&hl=en&tbs=qdr:w", searchPhrase.replaceAll(" ", "%20"));
    }

    public static String generatePlayerHashtags(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        String teamNameHashtag = "";
        if (playerMatchPerformanceStats.getMatch().getRelevantTeam() != null && !playerMatchPerformanceStats.getMatch().getRelevantTeam().isEmpty()) {
            teamNameHashtag = "#" + playerMatchPerformanceStats.getMatch().getRelevantTeam().replaceAll(" ", "");
        }
        return "#" + player.getName().replaceAll(" ", "") + " " +
                teamNameHashtag +  " " +
                "#womensfootball" + " " +
                "#womenssoccer";
    }
}
