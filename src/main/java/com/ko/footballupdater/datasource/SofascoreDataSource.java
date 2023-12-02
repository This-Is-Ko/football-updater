package com.ko.footballupdater.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Team;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Qualifier("sofascore")
public class SofascoreDataSource implements DataSourceParser {

    @Getter
    private final DataSourceSiteName dataSourceSiteName = DataSourceSiteName.SOFASCORE;

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private final String BASEURL = "https://api.sofascore.com";
    private final String API_MATCH_BASE_URL = "/api/v1/event";

    @Override
    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player, Document document) {
        return parsePlayerMatchData(player, document, null, false);
    }

    @Override
    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player, Document document, String url, boolean skipLatestMatchCheck) {
        // Incoming document should be from players event api
        // e.g. https://api.sofascore.com/api/v1/player/796007/events/last/0

        // Extract player id from url
        String sofascorePlayerId;
        Pattern pattern = Pattern.compile("/player/(\\d+)/");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            sofascorePlayerId = matcher.group(1);
        } else {
            log.atInfo().setMessage("Unable to extract player id from url").addKeyValue("player", player.getName()).log();
            return null;
        }

        try {
            JsonNode playerMatchesNode = new ObjectMapper().readTree(document.body().text());
            if (!playerMatchesNode.hasNonNull("events")) {
                log.atInfo().setMessage("Unable to find match list").addKeyValue("player", player.getName()).log();
                return null;
            }
            JsonNode matchList = playerMatchesNode.get("events");
            // Assume last in list is the latest match
            JsonNode matchEntry = matchList.get(matchList.size() - 1);
            if (!matchEntry.hasNonNull("id") && !matchEntry.get("id").textValue().isEmpty()) {
                log.atInfo().setMessage("Unable to find match id from match entry").addKeyValue("player", player.getName()).log();
                return null;
            }

            // Check match date against last checked
            Date selectedMatchDate = new Date(matchEntry.get("startTimestamp").intValue() * 1000L);
            // Skip for manual post generate calls
            if (!skipLatestMatchCheck) {
                if (player.getCheckedStatus().getLatestCheckedMatchDate() != null && !(selectedMatchDate.compareTo(player.getCheckedStatus().getLatestCheckedMatchDate()) > 0)) {
                    log.atInfo().setMessage("Selected match is not newer than last checked").addKeyValue("player", player.getName()).log();
                    return null;
                }
            }

            // Construct player match statistics url
            // e.g. https://api.sofascore.com/api/v1/event/11859193/player/796007/statistics
            int matchId = matchEntry.get("id").intValue();
            String matchStatisticsUrl = BASEURL + API_MATCH_BASE_URL + "/" + matchId + "/player/" + sofascorePlayerId + "/statistics";

            Document doc = Jsoup.connect(matchStatisticsUrl).ignoreContentType(true).get();
            JsonNode matchStatistics = new ObjectMapper().readTree(doc.body().text());
            if (!matchStatistics.hasNonNull("statistics")) {
                log.atInfo().setMessage("Unable to find match statistics").addKeyValue("player", player.getName()).log();
                return null;
            }
            JsonNode statistics = matchStatistics.get("statistics");

            // Separate processing for Goalkeeper stats
            PlayerMatchPerformanceStats playerMatchPerformanceStats;
            if (matchStatistics.hasNonNull("position") && matchStatistics.get("position").textValue().equals("G")) {
                playerMatchPerformanceStats = parseGoalkeeperMatchData(statistics);
            } else {
                playerMatchPerformanceStats = parseOutfieldPlayerMatchData(statistics);
            }

            // Retrieve yellow and red card stats - best attempt only
            // e.g. https://api.sofascore.com/api/v1/event/10834045/incidents
            try {
                String eventIncidentsUrl = BASEURL + API_MATCH_BASE_URL + "/" + matchId + "/incidents";
                Document eventIncidentsDoc = Jsoup.connect(eventIncidentsUrl).ignoreContentType(true).get();
                JsonNode eventIncidents = new ObjectMapper().readTree(eventIncidentsDoc.body().text());
                if (!eventIncidents.hasNonNull("incidents")) {
                    log.atInfo().setMessage("Unable to find match incidents").addKeyValue("player", player.getName()).log();
                    return null;
                }
                JsonNode incidents = eventIncidents.get("incidents");
                if (incidents.isArray()) {
                    for (JsonNode incident : incidents) {
                        if (incident.hasNonNull("player") && player.getName().equals(incident.get("player").get("name").textValue())) {
                            if (incident.get("incidentClass").textValue().equals("yellow")) {
                                playerMatchPerformanceStats.setYellowCards(playerMatchPerformanceStats.getYellowCards() + 1);
                            } else if (incident.get("incidentClass").textValue().equals("red")) {
                                playerMatchPerformanceStats.setRedCards(playerMatchPerformanceStats.getRedCards() + 1);
                            }
                        }
                    }
                }
                log.atInfo().setMessage("Updated player with yellow and red card stats").addKeyValue("player", player.getName()).log();
            } catch (Exception ex) {
                log.atWarn().setMessage("Unable to retrieve yellow and red card stats - ignoring as best attempt only").setCause(ex).addKeyValue("player", player.getName()).log();
            }

            // Store match data
            // Convert timestamp to date e.g. 1701487800 -> 2017-01-31T20:00:00.000Z
            Match match = new Match(url, selectedMatchDate, matchEntry.get("homeTeam").get("name").textValue(), matchEntry.get("awayTeam").get("name").textValue(), null);
            playerMatchPerformanceStats.setMatch(match);
            return playerMatchPerformanceStats;
        } catch (Exception ex) {
            log.atWarn().setMessage("Error while trying to update player").setCause(ex).addKeyValue("player", player.getName()).log();
        }
        return null;
    }

    private PlayerMatchPerformanceStats parseGoalkeeperMatchData(JsonNode statistics) {
        return PlayerMatchPerformanceStats.builder()
                .minutesPlayed(getStatIntegerOrDefault(statistics, "minutesPlayed"))
                .goals(getStatIntegerOrDefault(statistics, "goals"))
                .assists(getStatIntegerOrDefault(statistics, "assists"))
                .yellowCards(0)
                .redCards(0)
                .passesAttempted(getStatIntegerOrDefault(statistics, "totalPass"))
                .passesCompleted(getStatIntegerOrDefault(statistics, "accuratePass"))
                .longBallsAttempted(getStatIntegerOrDefault(statistics, "totalLongBalls"))
                .longBallsCompleted(getStatIntegerOrDefault(statistics, "accurateLongBalls"))
                .gkSaves(getStatIntegerOrDefault(statistics, "saves"))
                .touches(getStatIntegerOrDefault(statistics, "touches"))
                .build();
    }

    private PlayerMatchPerformanceStats parseOutfieldPlayerMatchData(JsonNode statistics) {
        return PlayerMatchPerformanceStats.builder()
                .minutesPlayed(getStatIntegerOrDefault(statistics, "minutesPlayed"))
                .goals(getStatIntegerOrDefault(statistics, "goals"))
                .assists(getStatIntegerOrDefault(statistics, "assists"))
                .yellowCards(0)
                .redCards(0)
                .passesAttempted(getStatIntegerOrDefault(statistics, "totalPass"))
                .passesCompleted(getStatIntegerOrDefault(statistics, "accuratePass"))
                .longBallsAttempted(getStatIntegerOrDefault(statistics, "totalLongBalls"))
                .longBallsCompleted(getStatIntegerOrDefault(statistics, "accurateLongBalls"))
                .crosses(getStatIntegerOrDefault(statistics, "totalCross"))
                .crossesSuccessful(getStatIntegerOrDefault(statistics, "accurateCross"))
                .duelsWon(getStatIntegerOrDefault(statistics, "duelWon"))
                .duelsLost(getStatIntegerOrDefault(statistics, "duelLost"))
                .aerialDuelsWon(getStatIntegerOrDefault(statistics, "aerialWon"))
//                    .aerialDuelsLost(getStatIntegerOrDefault(statistics, "aerialLost"))
                .aerialDuelsWon(getStatIntegerOrDefault(statistics, "accurateCross"))
                .tacklesWon(getStatIntegerOrDefault(statistics, "challengeWon"))
                .dispossessed(getStatIntegerOrDefault(statistics, "dispossessed"))
                .shots(getStatIntegerOrDefault(statistics, "onTargetScoringAttempt") + getStatIntegerOrDefault(statistics, "shotOffTarget"))
                .shotsOnTarget(getStatIntegerOrDefault(statistics, "onTargetScoringAttempt"))
                .shotsBlocked(getStatIntegerOrDefault(statistics, "blockedScoringAttempt"))
                .interceptions(getStatIntegerOrDefault(statistics, "interceptionWon"))
                .tackles(getStatIntegerOrDefault(statistics, "totalTackle"))
                .fouled(getStatIntegerOrDefault(statistics, "wasFouled"))
                .fouls(getStatIntegerOrDefault(statistics, "fouls"))
                .touches(getStatIntegerOrDefault(statistics, "touches"))
                .build();
    }

    private int getStatIntegerOrDefault(JsonNode statContainer, String stateName) {
        // Return default if stat is no found
        try {
            return statContainer.get(stateName).intValue();
        } catch (Exception ex) {
            return 0;
        }
    }

    @Override
    public void parseSquadDataForTeam(Team team, DataSource dataSource, List<Player> players) {
    }
}
