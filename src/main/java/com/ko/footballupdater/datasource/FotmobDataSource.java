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
import com.ko.footballupdater.utils.StatHelper;
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

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private final String BASEURL = "https://www.fotmob.com";
    private final String API_MATCH_BASE_URL = "/api/matchDetails?matchId=";
    private final String GOALKEEPER_FOTMOB_STRING_SHORT = "GK";

    @Override
    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player, Document document) {
        return parsePlayerMatchData(player, document, null, false);
    }

    @Override
    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player, Document document, String url, boolean skipLatestMatchCheck) {
        try {
            String latestMatchUrl;
            if (url != null && url.contains("/api/")) {
                // Datasource is api endpoint
                // Get player data from api endpoint
                // e.g. https://www.fotmob.com/api/newPlayerData?id=645995
                Element jsonElement =  document.selectFirst("body");
                if (jsonElement == null) {
                    return null;
                }
                String json = jsonElement.text();

                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(json);

                // Find most recent match - formatted as /matches/ol-reign-vs-san-diego-wave-fc/jyrgdzux#4351554
                latestMatchUrl = jsonNode.get("recentMatches").get(0).get("matchPageUrl").textValue();
            } else {
                // Datasource is frontend page
                // need to get match id from player page first
                // e.g. https://www.fotmob.com/players/645995/hayley-raso
                // Assume first row is the latest match
                Elements latestMatchRow = document.selectXpath("//main/div[2]/div[1]/div[4]/section/div/article/table/tbody/tr[1]/td[2]/a");
                if (latestMatchRow.isEmpty()) {
                    log.atInfo().setMessage("Cannot find any match results on player page").addKeyValue("player", player.getName()).log();
                    return null;
                }

                // Extract match id from url
                latestMatchUrl = latestMatchRow.get(0).attr("href");
            }

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
            // Skip for manual post generate calls
            if (!skipLatestMatchCheck) {
                if (player.getCheckedStatus().getLatestCheckedMatchDate() != null && !(selectedMatchDate.compareTo(player.getCheckedStatus().getLatestCheckedMatchDate()) > 0)) {
                    log.atInfo().setMessage("Selected match is not newer than last checked").addKeyValue("player", player.getName()).log();
                    return null;
                }
            }

            Match match = new Match(
                    matchReportUrl,
                    selectedMatchDate,
                    jsonNode.get("general").get("homeTeam").get("name").textValue(),
                    jsonNode.get("general").get("awayTeam").get("name").textValue(),
                    "");

            // Save match scores if available
            if (jsonNode.hasNonNull("header") && jsonNode.get("header").hasNonNull("teams") && jsonNode.get("header").get("teams").isArray()) {
                for (JsonNode teamEntry : jsonNode.get("header").get("teams")) {
                    if (teamEntry.hasNonNull("name") && teamEntry.hasNonNull("score")) {
                        if (match.getHomeTeamName() != null && match.getHomeTeamName().equals(teamEntry.get("name").textValue())) {
                            match.setHomeTeamScore(teamEntry.get("score").intValue());
                        } else if (match.getAwayTeamName() != null && match.getAwayTeamName().equals(teamEntry.get("name").textValue())) {
                            match.setAwayTeamScore(teamEntry.get("score").intValue());
                        }
                    }
                }
            }

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
                                        populateRelevantTeam(lineup, playerMatchPerformanceStats);
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
                                    populateRelevantTeam(lineup, playerMatchPerformanceStats);
                                    return playerMatchPerformanceStats;
                                }
                            }
                        }
                    } else {
                        // No Opta linup; use fallback
                        // Check starting lineup
                        JsonNode starting = lineup.get("players");
                        if (starting.isArray()) {
                            for (JsonNode formationLine : starting) {
                                for (JsonNode playerEntry : formationLine) {
                                    // Match player
                                    PlayerMatchPerformanceStats playerMatchPerformanceStats = checkPlayerAndParse(player, playerEntry, match);
                                    if (playerMatchPerformanceStats != null) {
                                        populateRelevantTeam(lineup, playerMatchPerformanceStats);
                                        return playerMatchPerformanceStats;
                                    }
                                }
                            }
                        }
                        // Check bench
                        JsonNode bench = lineup.get("bench");
                        if (bench.isArray()) {
                            for (JsonNode playerEntry : bench) {
                                // Match player
                                PlayerMatchPerformanceStats playerMatchPerformanceStats = checkPlayerAndParse(player, playerEntry, match);
                                if (playerMatchPerformanceStats != null) {
                                    populateRelevantTeam(lineup, playerMatchPerformanceStats);
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
        if (!player.getAllPossibleNames().contains(playerEntry.get("name").get("fullName").asText())) {
            return null;
        }
        JsonNode stats = playerEntry.get("stats");
        JsonNode topStats = null;
        JsonNode attackStats = null;
        JsonNode defenseStats = null;
        JsonNode duelsStats = null;

        if (stats != null && stats.isArray()) {
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
        } else {
            // Doesn't contain stat array; check for goals and minutes played
            int goals = 0;
            int yellowCard = 0;
            int redCard = 0;
            if (playerEntry.get("events") != null) {
                // Goals
                if (playerEntry.get("events").get("g") != null) {
                    goals = playerEntry.get("events").get("g").intValue();
                }
                if (playerEntry.get("events").get("yc") != null) {
                    yellowCard = playerEntry.get("events").get("yc").intValue();
                }
                if (playerEntry.get("events").get("rc") != null) {
                    redCard = playerEntry.get("events").get("rc").intValue();
                }
            }

            int minutesPlayed;
            int subbedOn = 0;
            int subbedOff = 90;
            if (playerEntry.get("timeSubbedOn") != null && playerEntry.hasNonNull("timeSubbedOn")) {
                subbedOn = playerEntry.get("timeSubbedOn").intValue();
            }
            if (playerEntry.get("timeSubbedOff") != null && playerEntry.hasNonNull("timeSubbedOff")) {
                subbedOff = playerEntry.get("timeSubbedOff").intValue();
            }
            minutesPlayed = subbedOff - subbedOn;

            return PlayerMatchPerformanceStats.builder()
                    .dataSourceSiteName(dataSourceSiteName)
                    .match(match)
                    .minutesPlayed(minutesPlayed)
                    .goals(goals)
                    .yellowCards(yellowCard)
                    .redCards(redCard)
                    .build();
        }

        // Separate stats required for goalkeepers
        if (playerEntry.get("positionStringShort") != null && GOALKEEPER_FOTMOB_STRING_SHORT.equals(playerEntry.get("positionStringShort").asText())) {
            // Goalkeepers only contain top stats
            if (topStats == null) {
                return null;
            }
            return PlayerMatchPerformanceStats.builder()
                    .dataSourceSiteName(dataSourceSiteName)
                    .match(match)
                    .minutesPlayed(getStatIntegerOrDefault(topStats, "Minutes played"))
                    .touches(getStatIntegerOrDefault(topStats, "Touches"))
                    .passesAttempted(parseFractionWithPercentageStatType(topStats, "Accurate passes", true))
                    .passesCompleted(parseFractionWithPercentageStatType(topStats, "Accurate passes", false))
                    .longBallsAttempted(parseFractionWithPercentageStatType(topStats, "Accurate long balls", true))
                    .longBallsCompleted(parseFractionWithPercentageStatType(topStats, "Accurate long balls", false))
                    .gkGoalsAgainst(getStatIntegerOrDefault(topStats, "Goals conceded"))
                    .gkSaves(getStatIntegerOrDefault(topStats, "Saves"))
                    .gkPunches(getStatIntegerOrDefault(topStats, "Punches"))
                    .gkThrows(getStatIntegerOrDefault(topStats, "Throws"))
                    .gkHighClaim(getStatIntegerOrDefault(topStats, "High claim"))
                    .gkRecoveries(getStatIntegerOrDefault(topStats, "Recoveries"))
                    .build();
        }

        // Outfield players require all stats
        if (topStats == null || attackStats == null || defenseStats == null || duelsStats == null) {
            log.atInfo().setMessage("Cannot find stats for outfield player").addKeyValue("player", player.getName()).log();
            return null;
        }

        // Yellow and red card are stored under events
        int yellowCard = 0;
        int redCard = 0;
        if (playerEntry.get("events") != null) {
            if (playerEntry.get("events").get("yc") != null) {
                yellowCard = playerEntry.get("events").get("yc").intValue();
            }
            if (playerEntry.get("events").get("rc") != null) {
                redCard = playerEntry.get("events").get("rc").intValue();
            }
        }


        PlayerMatchPerformanceStats playerMatchPerformanceStats = PlayerMatchPerformanceStats.builder()
                .dataSourceSiteName(dataSourceSiteName)
                .match(match)
                .minutesPlayed(getStatIntegerOrDefault(topStats, "Minutes played"))
                .goals(getStatIntegerOrDefault(topStats, "Goals"))
                .assists(getStatIntegerOrDefault(topStats, "Assists"))
                .yellowCards(yellowCard)
                .redCards(redCard)
                .shots(getStatIntegerOrDefault(topStats, "Total shots"))
                .shotsBlocked(getStatIntegerOrDefault(attackStats, "Blocked shots"))
                .fouls(getStatIntegerOrDefault(duelsStats, "Fouls committed"))
                .fouled(getStatIntegerOrDefault(duelsStats, "Was fouled"))
                .offsides(getStatIntegerOrDefault(attackStats, "Offsides"))
                .crosses(parseFractionWithPercentageStatType(attackStats, "Accurate crosses", true))
                .crossesSuccessful(parseFractionWithPercentageStatType(attackStats, "Accurate crosses", false))
                .dispossessed(getStatIntegerOrDefault(attackStats, "Dispossessed"))
                .touches(getStatIntegerOrDefault(attackStats, "Touches"))
                .tackles(parseFractionWithPercentageStatType(defenseStats, "Tackles won", true))
                .tacklesWon(parseFractionWithPercentageStatType(defenseStats, "Tackles won", false))
                .defensiveActions(getStatIntegerOrDefault(defenseStats, "Defensive actions"))
                .recoveries(getStatIntegerOrDefault(defenseStats, "Recoveries"))
                .duelsWon(getStatIntegerOrDefault(duelsStats, "Duels won"))
                .duelsLost(getStatIntegerOrDefault(duelsStats, "Duels lost"))
                .groundDuelsWon(getStatIntegerOrDefault(duelsStats, "Ground duels won"))
                .aerialDuelsWon(getStatIntegerOrDefault(duelsStats, "Aerial duels won"))
                .chancesCreatedAll(getStatIntegerOrDefault(topStats, "Chances created"))
                .passesAttempted(parseFractionWithPercentageStatType(topStats, "Accurate passes", true))
                .passesCompleted(parseFractionWithPercentageStatType(topStats, "Accurate passes", false))
                .passesIntoFinalThird(getStatIntegerOrDefault(attackStats, "Passes into final third"))
                .carries(parseFractionWithPercentageStatType(attackStats, "Successful dribbles", true))
                .carriesSuccessful(parseFractionWithPercentageStatType(attackStats, "Successful dribbles", false))
                .build();

        StatHelper.populateStatPercentages(playerMatchPerformanceStats);

        return playerMatchPerformanceStats;
    }

    private int getStatIntegerOrDefault(JsonNode statContainer, String stateName) {
        // Return default if stat is no found
        try {
            return statContainer.get("stats").get(stateName).get("stat").get("value").intValue();
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * Example entry
     * {"key":"shot_accuracy","stat":{"value":1,"total":2,"type":"fractionWithPercentage"}}
     */
    private Integer parseFractionWithPercentageStatType(JsonNode statContainer, String stateName, boolean isTotal) {
        // Return default if stat is no found
        try {
            if (isTotal) {
                return statContainer.get("stats").get(stateName).get("stat").get("total").intValue();
            }
            return statContainer.get("stats").get(stateName).get("stat").get("value").intValue();
        } catch (Exception ex) {
            return 0;
        }
    }

    private void populateRelevantTeam(JsonNode lineupObject, PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        if (lineupObject.hasNonNull("teamName")) {
            playerMatchPerformanceStats.getMatch().setRelevantTeam(lineupObject.get("teamName").asText());
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

    private String generatePlayerUrl(JsonNode playerEntry, String playerName) throws IllegalArgumentException {
        // Example url
        // https://www.fotmob.com/players/645998/mackenzie-arnold
        int playerId = playerEntry.get("id").intValue();
        if (playerId != 0) {
            return BASEURL + "/players/" + playerId + "/" + playerName.replaceAll(" ", "-");
        } else {
            throw new IllegalArgumentException("Unable to generate player url due to href empty: " + playerName);
        }
    }
}
