package com.ko.footballupdater.utils;

import com.ko.footballupdater.models.Hashtag;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.repositories.TeamRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PostHelper {
    @Autowired
    private TeamRepository teamRepository;

    // Generate caption based on post version
    // v1 All stats in caption
    // v2 Only name, match, date, hashtags in caption
    public void generatePostCaption(int version, Post postHolder, String additionalHashtags) {
        String caption;
        if (version == 2) {
            caption = generatePostDefaultPlayerCaptionV2(postHolder.getPlayer(), postHolder.getPlayerMatchPerformanceStats(), additionalHashtags);
        } else {
            caption = generatePostDefaultPlayerCaption(postHolder.getPlayer(), postHolder.getPlayerMatchPerformanceStats(), additionalHashtags);
        }
        postHolder.setCaption(caption);
    }

    // V1
    public String generatePostDefaultPlayerCaption(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats, String additionalHashtags) {
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
    public String generatePostDefaultPlayerCaptionV2(Player player, PlayerMatchPerformanceStats playerMatchPerformanceStats, String additionalHashtags) {
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

    public void generatePostImageSearchUrl(Post post) {
        String relevantTeam = "";
        if (post.getPlayerMatchPerformanceStats() != null && post.getPlayerMatchPerformanceStats().getMatch() != null) {
            relevantTeam = post.getPlayerMatchPerformanceStats().getMatch().getRelevantTeam();
        }
        String searchPhrase = post.getPlayer().getName() + " " + relevantTeam;
        post.getImageSearchUrls().add(String.format("https://www.google.com/search?q=%s&tbm=isch&hl=en&tbs=qdr:d", searchPhrase.replaceAll(" ", "%20")));
        post.getImageSearchUrls().add(String.format("https://www.google.com/search?q=%s&tbm=isch&hl=en&tbs=qdr:w", searchPhrase.replaceAll(" ", "%20")));
    }

    public String generatePlayerHashtags(Player player) {
        return "\n\n#" + player.getName().replaceAll(" ", "").replaceAll("-", "").toLowerCase();
    }

    public String generateTeamHashtags(String teamName) {
        StringBuilder teamHashtags = new StringBuilder(" ");
        if (teamName == null || teamName.isEmpty()) {
            return teamHashtags.toString();
        }

        // Search for team with name
        List<Team> playerTeams = teamRepository.findByName(teamName);
        if (playerTeams.size() == 1) {
            Team playerTeam = playerTeams.get(0);
            if (playerTeam != null) {
                teamHashtags.append(
                        playerTeam.getAdditionalHashtags().stream()
                                .map(Hashtag::getValue)
                                .collect(Collectors.joining(", "))
                );
            }
        } else {
            // Search for team with name in alternative name field, if not found, generate alternative name hashtag
            List<Team> playerTeamsAltName = teamRepository.findByAlternativeTeamName(teamName.toLowerCase());
            if (playerTeamsAltName.size() == 1) {
                Team playerTeam = playerTeamsAltName.get(0);
                if (playerTeam != null) {
                    teamHashtags.append(
                            playerTeam.getAdditionalHashtags().stream()
                                    .map(Hashtag::getValue)
                                    .collect(Collectors.joining(" "))
                    );
                }
            } else {
                teamHashtags.append("#").append(teamName.replaceAll(" ", "").replaceAll("-", "").replaceAll("\\.", ""));
            }
        }

        return teamHashtags.toString().replaceAll(",", "").toLowerCase();
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
