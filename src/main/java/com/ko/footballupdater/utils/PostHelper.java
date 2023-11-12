package com.ko.footballupdater.utils;

import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;

public class PostHelper {

    // Generate caption based on post version
    // v1 All stats in caption
    // v2 Only name, match, date, hashtags in caption
    public static void generatePostCaption(int version, Post postHolder) {
        String caption = "";
        if (version == 2) {
            caption = generatePostDefaultPlayerCaptionV2(postHolder.getPlayer(), postHolder.getPlayerMatchPerformanceStats());
        } else {
            caption = generatePostDefaultPlayerCaption(postHolder.getPlayer(), postHolder.getPlayerMatchPerformanceStats());
        }
        postHolder.setCaption(caption);
    }

    // V1
    public static String generatePostDefaultPlayerCaption(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        return String.format("%s stats in %s vs %s on %s\n",
                player.getName(),
                playerMatchPerformanceStats.getMatch().getHomeTeamName(),
                playerMatchPerformanceStats.getMatch().getAwayTeamName(),
                DateTimeHelper.getDateAsFormattedString(playerMatchPerformanceStats.getMatch().getDate())
        ) + playerMatchPerformanceStats.toFormattedString() + generatePlayerHashtags(player, playerMatchPerformanceStats);
    }

    // V2
    public static String generatePostDefaultPlayerCaptionV2(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        return String.format("%s stats in %s vs %s on %s\n",
                player.getName(),
                playerMatchPerformanceStats.getMatch().getHomeTeamName(),
                playerMatchPerformanceStats.getMatch().getAwayTeamName(),
                DateTimeHelper.getDateAsFormattedString(playerMatchPerformanceStats.getMatch().getDate())
        ) + generatePlayerHashtags(player, playerMatchPerformanceStats);
    }

    public static String generateMatchName(PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        return String.format("%s VS %s",
                playerMatchPerformanceStats.getMatch().getHomeTeamName(),
                playerMatchPerformanceStats.getMatch().getAwayTeamName()
        );
    }

    public static void generatePostImageSearchUrl(Post post) {
        String relevantTeam = "";
        if (post.getPlayerMatchPerformanceStats() != null && post.getPlayerMatchPerformanceStats().getMatch() != null) {
            relevantTeam = post.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam();
        }
        String searchPhrase = post.getPlayer().getName() + " " + relevantTeam;
        post.getImageSearchUrls().add(String.format("https://www.google.com/search?q=%s&tbm=isch&hl=en&tbs=qdr:d", searchPhrase.replaceAll(" ", "%20")));
        post.getImageSearchUrls().add(String.format("https://www.google.com/search?q=%s&tbm=isch&hl=en&tbs=qdr:w", searchPhrase.replaceAll(" ", "%20")));
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

    public static String generateS3UrlList(Post postHolder) {
        StringBuilder builder = new StringBuilder();
        if (!postHolder.getImagesUrls().isEmpty()) {
            for (String url : postHolder.getImagesUrls()) {
                builder.append(url).append("\n");
            }
        }
        return builder.toString();
    }
}
