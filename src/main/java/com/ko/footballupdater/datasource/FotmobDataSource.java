package com.ko.footballupdater.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.net.URL;

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

import static com.ko.footballupdater.datasource.ParsingHelper.parseFloatOrNull;
import static com.ko.footballupdater.datasource.ParsingHelper.parseIntegerOrNull;

@Slf4j
@Component
@Qualifier("fotmob")
public class FotmobDataSource implements DataSourceParser {

    @Getter
    private final DataSourceSiteName dataSourceSiteName = DataSourceSiteName.FOTMOB;

    private final String HOME = "Home";
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private final String BASEURL = "https://www.fotmob.com/";
    private final String API_MATCH_BASE_URL = "/api/matchDetails?matchId=";

    @Override
    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player, Document document) {
        try {
            // Fotmob - need to get match id from player page first
            // e.g. https://www.fotmob.com/players/645995/hayley-raso

            // Assume first row is the latest match
            Elements latestMatchRow = document.selectXpath("//main/div[2]/div[1]/div[4]/section/div/article/table/tbody/tr[1]/td[2]/a");
            if (latestMatchRow.isEmpty()) {
                log.info("Cannot find any match results on player page");
                return null;
            }

            // Extract match id from url
            String latestMatchUrl = latestMatchRow.get(0).attr("href");

            Pattern pattern = Pattern.compile("/match/(\\d+)/");
            Matcher matcher = pattern.matcher(latestMatchUrl);
            if (!matcher.find()) {
                return null;
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

            Match match = new Match(
                    matchReportUrl,
                    dateFormat.parse(jsonNode.get("general").get("matchTimeUTCDate").textValue()),
                    jsonNode.get("general").get("homeTeam").get("name").textValue(),
                    jsonNode.get("general").get("awayTeam").get("name").textValue(),
                    "");

            JsonNode lineups = jsonNode.get("content").get("lineup").get("lineup");
            if (lineups.isArray()) {
                for (JsonNode lineup : lineups) {
                    JsonNode optaLineup = lineup.get("optaLineup");
                    if (optaLineup.isObject()) {
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
            log.atInfo().setMessage(player.getName() + " " + "Unable to update player, parsed all players in the match").addKeyValue("player", player.getName()).log();
        } catch (Exception ex) {
            log.warn("Error while trying to update player: " + player.getName() + " - " + ex);
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

        if (topStats == null || attackStats == null || defenseStats == null || duelsStats == null) {
            return null;
        }

        return new PlayerMatchPerformanceStats(
            match,
            topStats.get("stats").get("Minutes played").get("value").intValue(),
            topStats.get("stats").get("Goals").get("value").intValue(),
            topStats.get("stats").get("Assists").get("value").intValue(),
            topStats.get("stats").get("Total shots").get("value").intValue(),
            attackStats.get("stats").get("Blocked shots").get("value").intValue(),
            duelsStats.get("stats").get("Fouls committed").get("value").intValue(),
            duelsStats.get("stats").get("Was fouled").get("value").intValue(),
            attackStats.get("stats").get("Offsides").get("value").intValue(),
            attackStats.get("stats").get("Accurate crosses").get("value").textValue(),
            attackStats.get("stats").get("Dispossessed").get("value").intValue(),
            attackStats.get("stats").get("Touches").get("value").intValue(),
            defenseStats.get("stats").get("Tackles won").get("value").textValue(),
            defenseStats.get("stats").get("Defensive actions").get("value").intValue(),
            defenseStats.get("stats").get("Recoveries").get("value").intValue(),
            duelsStats.get("stats").get("Duels won").get("value").intValue(),
            duelsStats.get("stats").get("Duels lost").get("value").intValue(),
            duelsStats.get("stats").get("Ground duels won").get("value").intValue(),
            duelsStats.get("stats").get("Aerial duels won").get("value").intValue(),
            topStats.get("stats").get("Chances created").get("value").intValue(),
            topStats.get("stats").get("Accurate passes").get("value").textValue(),
            attackStats.get("stats").get("Passes into final third").get("value").intValue(),
            attackStats.get("stats").get("Successful dribbles").get("value").textValue()
            );
    }

    @Override
    public List<Player> parseSquadDataForTeam(Team team, DataSource dataSource) {
//        List<Player> players = new ArrayList<>();
//        try {
//            Document doc = Jsoup.connect(dataSource.getUrl()).get();
//
//            Element tableElement = doc.getElementById("roster");
//            if (tableElement == null) {
//                log.info("Unable to find roster/squad: table");
//                return null;
//            }
//            Element tbodyElement = tableElement.getElementsByTag("tbody").first();
//            if (tbodyElement == null) {
//                log.info("Cannot find any match results: tbody");
//                return null;
//            }
//            Elements playerRows = tbodyElement.select("tr");
//            if (playerRows.isEmpty()) {
//                log.info("Cannot find any match results in table: tr");
//                return null;
//            }
//            for (Element playerRow : playerRows) {
//                Player player = new Player(
//                        playerRow.select("td[data-stat=player] > a").text(),
//                        dateFormat.parse(playerRow.select("td[data-stat=birth_date]").text())
//                );
//                Set<DataSource> dataSources = new HashSet<>();
//                dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FBREF, generatePlayerUrl(playerRow, player.getName())));
//                player.setDataSources(dataSources);
//                players.add(player);
//            }
//            return players;
//        } catch (Exception ex) {
//            log.warn("Unable to create players list to add: " + ex);
//            return null;
//        }
        return null;
    }

}
