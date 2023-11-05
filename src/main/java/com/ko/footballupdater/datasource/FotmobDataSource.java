package com.ko.footballupdater.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.DataSourceType;
import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Team;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Qualifier("fotmob")
public class FotmobDataSource implements DataSourceParser {

    @Getter
    private final DataSourceSiteName dataSourceSiteName = DataSourceSiteName.FOTMOB;

    private final String HOME = "Home";
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private final String BASEURL = "https://www.fotmob.com";
    private final String API_MATCH_BASE_URL = "/api/matchDetails?matchId=";
    private final String GOALKEEPER_FOTMOB_STRING_SHORT = "GK";

    @Override
    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player, Document document) {
        try {
            // Fotmob - need to get match id from player page first
            // e.g. https://www.fotmob.com/players/645995/hayley-raso

            // Assume first row is the latest match
            Elements latestMatchRow = document.selectXpath("//main/div[2]/div[1]/div[4]/section/div/article/table/tbody/tr[1]/td[2]/a");
            if (latestMatchRow.isEmpty()) {
                log.atInfo().setMessage("Cannot find any match results on player page").addKeyValue("player", player.getName()).log();
                return null;
            }

            // Extract match id from url
            String latestMatchUrl = latestMatchRow.get(0).attr("href");

            Pattern pattern = Pattern.compile("/match/(\\d+)/");
            Matcher matcher = pattern.matcher(latestMatchUrl);
            if (!matcher.find()) {
                pattern = Pattern.compile("/matches/.*/NaN#(\\d+)");
                matcher = pattern.matcher(latestMatchUrl);
                if (!matcher.find()) {
                    pattern = Pattern.compile("/matches/.*#(\\d+)");
                    matcher = pattern.matcher(latestMatchUrl);
                    if (!matcher.find()) {
                        log.atInfo().setMessage("Cannot find match id from url").addKeyValue("player", player.getName()).log();
                        return null;
                    }
                }
            }

            // Construct api url
            // e.g. https://www.fotmob.com/api/matchDetails?matchId=4271090
            String latestMatchId = matcher.group(1);
            String matchReportUrl = BASEURL + API_MATCH_BASE_URL + latestMatchId;

            Document matchReportDocument = Jsoup.connect(matchReportUrl).ignoreContentType(true).get();
            Element jsonElement =  matchReportDocument.selectFirst("body");
            if (jsonElement == null) {
                return null;
            }
            String json = jsonElement.text();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(json);

            // Check whether this match is newer than last checked
            Date selectedMatchDate = dateFormat.parse(jsonNode.get("general").get("matchTimeUTCDate").textValue());
            if (player.getCheckedStatus().getLatestCheckedMatchDate() != null && !(selectedMatchDate.compareTo(player.getCheckedStatus().getLatestCheckedMatchDate()) > 0)) {
                log.atInfo().setMessage("Selected match is not newer than last checked").addKeyValue("player", player.getName()).log();
                return null;
            }

            Match match = new Match(
                    matchReportUrl,
                    selectedMatchDate,
                    jsonNode.get("general").get("homeTeam").get("name").textValue(),
                    jsonNode.get("general").get("awayTeam").get("name").textValue(),
                    "");

            JsonNode lineups = jsonNode.get("content").get("lineup").get("lineup");
            if (lineups.isArray()) {
                for (JsonNode lineup : lineups) {
                    JsonNode optaLineup = lineup.get("optaLineup");
                    if (optaLineup != null && optaLineup.isObject()) {
                        // Check starting lineup
                        JsonNode starting = optaLineup.get("players");
                        if (starting.isArray()) {
                            for (JsonNode formationLine : starting) {
                                for (JsonNode playerEntry : formationLine) {
                                    // Match player
                                    PlayerMatchPerformanceStats playerMatchPerformanceStats = checkPlayerAndParse(player, playerEntry, match);
                                    if (playerMatchPerformanceStats != null) {
                                        return playerMatchPerformanceStats;
                                    }
                                }
                            }
                        }
                        // Check bench
                        JsonNode bench = optaLineup.get("bench");
                        if (bench.isArray()) {
                            for (JsonNode playerEntry : bench) {
                                // Match player
                                PlayerMatchPerformanceStats playerMatchPerformanceStats = checkPlayerAndParse(player, playerEntry, match);
                                if (playerMatchPerformanceStats != null) {
                                    return playerMatchPerformanceStats;
                                }
                            }
                        }
                    }
                }
            }
            log.atInfo().setMessage("Unable to update player, parsed all players in the match").addKeyValue("player", player.getName()).log();
        } catch (Exception ex) {
            log.atWarn().setMessage("Error while trying to update player").setCause(ex).addKeyValue("player", player.getName()).log();
        }
        return null;
    }

    private PlayerMatchPerformanceStats checkPlayerAndParse(Player player, JsonNode playerEntry, Match match) {
        if (!player.getName().equals(playerEntry.get("name").get("fullName").asText())) {
            return null;
        }
        JsonNode stats = playerEntry.get("stats");
        JsonNode topStats = null;
        JsonNode attackStats = null;
        JsonNode defenseStats = null;
        JsonNode duelsStats = null;

        if (stats.isArray()) {
            for (JsonNode statCategory : stats) {
                if ("top_stats".equals(statCategory.get("key").textValue())) {
                    topStats = statCategory;
                } else if ("attack".equals(statCategory.get("key").textValue())) {
                    attackStats = statCategory;
                } else if ("defense".equals(statCategory.get("key").textValue())) {
                    defenseStats = statCategory;
                } else if ("duels".equals(statCategory.get("key").textValue())) {
                    duelsStats = statCategory;
                }
            }
        }

        // Separate stats required for goalkeepers
        if (playerEntry.get("positionStringShort") != null && GOALKEEPER_FOTMOB_STRING_SHORT.equals(playerEntry.get("positionStringShort").asText())) {
            // Goalkeepers only contain top stats
            if (topStats == null) {
                return null;
            }
            return new PlayerMatchPerformanceStats(dataSourceSiteName,
                    match,
                    getStatIntegerOrDefault(topStats, "Minutes played", 0),
                    getStatIntegerOrDefault(topStats, "Touches", 0),
                    getStatStringOrDefault(topStats, "Accurate passes", "0"),
                    getStatStringOrDefault(topStats, "Accurate long balls", "0"),
                    getStatIntegerOrDefault(topStats, "Goals conceded", 0),
                    getStatStringOrDefault(topStats, "Saves", "0"),
                    getStatIntegerOrDefault(topStats, "Punches", 0),
                    getStatIntegerOrDefault(topStats, "Throws", 0),
                    getStatIntegerOrDefault(topStats, "High claim", 0),
                    getStatIntegerOrDefault(topStats, "Recoveries", 0)
                    );
        }

        // Outfield players require all stats
        if (topStats == null || attackStats == null || defenseStats == null || duelsStats == null) {
            log.atInfo().setMessage("Cannot find stats for outfield player").addKeyValue("player", player.getName()).log();
            return null;
        }

        return new PlayerMatchPerformanceStats(
            dataSourceSiteName,
            match,
            getStatIntegerOrDefault(topStats, "Minutes played", 0),
            getStatIntegerOrDefault(topStats, "Goals", 0),
            getStatIntegerOrDefault(topStats, "Assists", 0),
            getStatIntegerOrDefault(topStats, "Total shots", 0),
            getStatIntegerOrDefault(attackStats, "Blocked shots", 0),
            getStatIntegerOrDefault(duelsStats, "Fouls committed", 0),
            getStatIntegerOrDefault(duelsStats, "Was fouled", 0),
            getStatIntegerOrDefault(attackStats, "Offsides", 0),
            getStatStringOrDefault(attackStats, "Accurate crosses", "0"),
            getStatIntegerOrDefault(attackStats, "Dispossessed", 0),
            getStatIntegerOrDefault(attackStats, "Touches", 0),
            getStatStringOrDefault(defenseStats, "Tackles won", "0"),
            getStatIntegerOrDefault(defenseStats, "Defensive actions", 0),
            getStatIntegerOrDefault(defenseStats, "Recoveries", 0),
            getStatIntegerOrDefault(duelsStats, "Duels won", 0),
            getStatIntegerOrDefault(duelsStats, "Duels lost", 0),
            getStatIntegerOrDefault(duelsStats, "Ground duels won", 0),
            getStatIntegerOrDefault(duelsStats, "Aerial duels won", 0),
            getStatIntegerOrDefault(topStats, "Chances created", 0),
            getStatStringOrDefault(topStats, "Accurate passes", "0"),
            getStatIntegerOrDefault(attackStats, "Passes into final third", 0),
            getStatStringOrDefault(attackStats, "Successful dribbles", "0")
            );
    }

    private int getStatIntegerOrDefault(JsonNode statContainer, String stateName, int defaultValue) {
        // Return default if stat is no found
        try {
            return statContainer.get("stats").get(stateName).get("value").intValue();
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private String getStatStringOrDefault(JsonNode statContainer, String stateName, String defaultValue) {
        // Return default if stat is no found
        try {
            return statContainer.get("stats").get(stateName).get("value").textValue();
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    @Override
    public void parseSquadDataForTeam(Team team, DataSource dataSource, List<Player> players) {
        // https://www.fotmob.com/api/teams?id=5981&ccode3=AUS
        try {
            Document matchReportDocument = Jsoup.connect(dataSource.getUrl()).ignoreContentType(true).get();
            Element jsonElement =  matchReportDocument.selectFirst("body");
            if (jsonElement == null) {
                return;
            }
            String json = jsonElement.text();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(json);

            JsonNode squad = jsonNode.get("squad");
            if (squad == null) {
                log.info("Unable to find squad entries");
                return;
            }
            if (squad.isArray()) {
                for (JsonNode squadSection : squad) {
                    if (squadSection.size() != 2 || squadSection.get(0).textValue().equals("coach")) {
                        continue;
                    }
                    for (JsonNode playerEntry : squadSection.get(1)) {
                        String playerName = playerEntry.get("name").textValue();
                        DataSource newDataSource = new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FOTMOB, generatePlayerUrl(playerEntry, playerName));
                        // Player exists in passed player list
                        if (players.stream().anyMatch(o -> o.getName().equals(playerName))) {
                            Player existingPlayer = players.stream().filter(o -> o.getName().equals(playerName)).findFirst().get();
                            existingPlayer.getDataSources().add(newDataSource);
                        } else {
                            // Create player and add to list
                            Player player = new Player(playerName);
                            Set<DataSource> dataSources = new HashSet<>();
                            dataSources.add(newDataSource);
                            player.setDataSources(dataSources);
                            players.add(player);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Unable to create players list to add: " + ex);
        }
    }

    private String generatePlayerUrl(JsonNode playerEntry, String playerName) throws Exception {
        // Example url
        // https://www.fotmob.com/players/645998/mackenzie-arnold
        int playerId = playerEntry.get("id").intValue();
        if (playerId != 0) {
            return BASEURL + "/players/" + playerId + "/" + playerName.replaceAll(" ", "-");
        } else {
            throw new Exception("Unable to generate player url due to href empty: " + playerName);
        }
    }
}
