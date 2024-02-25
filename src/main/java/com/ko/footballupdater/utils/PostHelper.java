package com.ko.footballupdater.utils;

import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class PostHelper {

    // Generate caption based on post version
    // v1 All stats in caption
    // v2 Only name, match, date, hashtags in caption
    public static void generatePostCaption(int version, Post postHolder, String additionalHashtags) {
        String caption;
        if (version == 2) {
            caption = generatePostDefaultPlayerCaptionV2(postHolder.getPlayer(), postHolder.getPlayerMatchPerformanceStats(), additionalHashtags);
        } else {
            caption = generatePostDefaultPlayerCaption(postHolder.getPlayer(), postHolder.getPlayerMatchPerformanceStats(), additionalHashtags);
        }
        postHolder.setCaption(caption);
    }

    // V1
    public static String generatePostDefaultPlayerCaption(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats, String additionalHashtags) {
        return String.format("%s stats in %s vs %s on %s\n",
                player.getName(),
                playerMatchPerformanceStats.getMatch().getHomeTeamName(),
                playerMatchPerformanceStats.getMatch().getAwayTeamName(),
                DateTimeHelper.getDateAsFormattedString(playerMatchPerformanceStats.getMatch().getDate())
        )
                + playerMatchPerformanceStats.toFormattedString()
                + generatePlayerHashtags(player)
                + " " + additionalHashtags;
    }

    // V2
    public static String generatePostDefaultPlayerCaptionV2(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats, String additionalHashtags) {
        StringBuilder strBuilder = new StringBuilder();
        if (playerMatchPerformanceStats.getMatch() != null && playerMatchPerformanceStats.getMatch().getHomeTeamScore() != null && playerMatchPerformanceStats.getMatch().getAwayTeamScore() != null) {
            strBuilder.append(String.format("%s stats in %s vs %s", player.getName(),
                    playerMatchPerformanceStats.getMatch().getHomeTeamName(),
                    playerMatchPerformanceStats.getMatch().getAwayTeamName()));
            if (playerMatchPerformanceStats.getMatch().getHomeTeamScore() != null && playerMatchPerformanceStats.getMatch().getAwayTeamScore() != null) {
                strBuilder.append(String.format(" (%d-%d)",
                        playerMatchPerformanceStats.getMatch().getHomeTeamScore(),
                        playerMatchPerformanceStats.getMatch().getAwayTeamScore()));
            }
            if (playerMatchPerformanceStats.getMatch().getDate() != null) {
                strBuilder.append(String.format(" on %s",
                        DateTimeHelper.getDateAsFormattedString(playerMatchPerformanceStats.getMatch().getDate())));
            }
            strBuilder.append("\n");
        }
        strBuilder.append(generatePlayerHashtags(player)).append(" ").append(additionalHashtags);
        return strBuilder.toString();
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

    public static String generatePlayerHashtags(Player player) {
        return "\n\n#" + player.getName().replaceAll(" ", "").replaceAll("-", "").toLowerCase();
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

    public static boolean areHashtagsValid(ArrayList<String> hashtags) {
        if (hashtags != null) {
            for (String hashtag : hashtags) {
                if (!hashtag.startsWith("#") || hashtag.contains(" ") || hashtag.contains("\t")) {
                    log.atInfo().setMessage("Invalid hashtag - " + hashtag).log();
                    return false;
                }
            }
        }
        return true;
    }
}
