package com.ko.footballupdater.utils;

import com.ko.footballupdater.models.InstagramPostHolder;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;

public class PostHelper {

    // Generate caption based on post version
    // v1 All stats in caption
    // v2 Only name, match, date, hashtags in caption
    public static void generatePostCaption(int version, InstagramPostHolder postHolder) {
        String caption = "";
        if (version == 2) {
            caption = generatePostDefaultPlayerCaptionV2(postHolder.getPost().getPlayer(), postHolder.getPlayerMatchPerformanceStats());
        } else {
            caption = generatePostDefaultPlayerCaption(postHolder.getPost().getPlayer(), postHolder.getPlayerMatchPerformanceStats());
        }
        postHolder.getPost().setCaption(caption);
    }

    // V1
    public static String generatePostDefaultPlayerCaption(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        return String.format("%s stats in %s vs %s on %s\n",
                player.getName(),
                playerMatchPerformanceStats.getMatch().getHomeTeamName(),
                playerMatchPerformanceStats.getMatch().getAwayTeamName(),
                playerMatchPerformanceStats.getMatch().getDateAsFormattedString()
        ) + playerMatchPerformanceStats.toFormattedString() + generatePlayerHashtags(player, playerMatchPerformanceStats);
    }


    // V2
    public static String generatePostDefaultPlayerCaptionV2(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        return String.format("%s stats in %s vs %s on %s\n",
                player.getName(),
                playerMatchPerformanceStats.getMatch().getHomeTeamName(),
                playerMatchPerformanceStats.getMatch().getAwayTeamName(),
                playerMatchPerformanceStats.getMatch().getDateAsFormattedString()
        ) + generatePlayerHashtags(player, playerMatchPerformanceStats);
    }

    public static String generatePostImageSearchUrl(InstagramPostHolder postHolder) {
        String searchPhrase = postHolder.getPost().getPlayer().getName() + " " + postHolder.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam();
        return String.format("https://www.google.com/search?q=%s&tbm=isch&hl=en&tbs=qdr:d\n", searchPhrase.replaceAll(" ", "%20")) + String.format("https://www.google.com/search?q=%s&tbm=isch&hl=en&tbs=qdr:w", searchPhrase.replaceAll(" ", "%20"));
    }

    public static String generatePlayerHashtags(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        String teamNameHashtag = "";
        if (playerMatchPerformanceStats.getMatch().getRelevantTeam() != null && !playerMatchPerformanceStats.getMatch().getRelevantTeam().isEmpty()) {
            teamNameHashtag = "#" + playerMatchPerformanceStats.getMatch().getRelevantTeam().replaceAll(" ", "");
        }
        return "#" + player.getName().replaceAll(" ", "") + " " +
                "#upthetillies " +
                "#matildas " +
                "#womensfootball " +
                "#womenssoccer" +
                "#woso " +
                teamNameHashtag;
    }

    public static String generateS3UrlList(InstagramPostHolder postHolder) {
        StringBuilder builder = new StringBuilder();
        if (!postHolder.getPost().getImagesUrls().isEmpty()) {
            for (String url : postHolder.getPost().getImagesUrls()) {
                builder.append(url).append("\n");
            }
        }
        return builder.toString();
    }
}
