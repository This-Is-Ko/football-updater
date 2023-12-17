package com.ko.footballupdater.datasource;

import com.ko.footballupdater.exceptions.ParsingException;
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
@Qualifier("fbref")
public class FbrefDataSource implements DataSourceParser {

    @Getter
    private final DataSourceSiteName dataSourceSiteName = DataSourceSiteName.FBREF;

    private final String HOME = "Home";
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final String BASEURL = "https://fbref.com";
    private final String PATH_REGEX = "/en/players/[0-9a-zA-Z]+";
    private final String PATH_SUFFIX = "/matchlogs/2023-2024";


    @Override
    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player, Document document) {
        return parsePlayerMatchData(player, document, null, false);
    }

    @Override
    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player, Document document, String url, boolean skipLatestMatchCheck) {
        try {
            Element tableElement = document.getElementsByClass("stats_table").first();
            if (tableElement == null) {
                log.info("Cannot find any match results table: stats_table");
                return null;
            }
            Element tbodyElement = tableElement.getElementsByTag("tbody").first();
            if (tbodyElement == null) {
                log.info("Cannot find any match results: tbody");
                return null;
            }
            Elements resultRows = tbodyElement.select("tr");
            if (resultRows.isEmpty()) {
                log.info("Cannot find any match results in table: tr");
                return null;
            }
            // Assume last row is the latest match
            Collections.reverse(resultRows);
            for (Element resultRow : resultRows) {
                // For games not played, appears as unused_sub class
                if (!resultRow.getElementsByClass("unused_sub").isEmpty()) {
                    log.atInfo().setMessage("Skip latest due to unused sub").addKeyValue("player", player.getName()).log();
                    continue;
                } else if (!resultRow.getElementsByClass("partial_table").isEmpty()) {
                    log.atInfo().setMessage("Spacer row skipped").addKeyValue("player", player.getName()).log();
                    continue;
                }
                String latestMatchUrl = resultRow.select("th[data-stat=date] > a").attr("href");

                // Check if match is new
                Date selectedMatchDate;
                if (!resultRow.select("th[data-stat=date] > a").text().isEmpty()) {
                    selectedMatchDate = dateFormat.parse(resultRow.select("th[data-stat=date] > a").text());
                } else {
                    log.atInfo().setMessage("Unable to get date from match row").addKeyValue("player", player.getName()).log();
                    return null;
                }

                // Skip for manual post generate calls
                if (!skipLatestMatchCheck) {
                    if (player.getCheckedStatus() != null) {
                        if (player.getCheckedStatus().getLatestCheckedMatchDate() != null && !(selectedMatchDate.compareTo(player.getCheckedStatus().getLatestCheckedMatchDate()) > 0)) {
                            log.atInfo().setMessage("Selected match is not newer than last checked").addKeyValue("player", player.getName()).log();
                            return null;
                        } else if (player.getCheckedStatus().getLatestCheckedMatchUrl() != null && player.getCheckedStatus().getLatestCheckedMatchUrl().equals(latestMatchUrl)) {
                            // No new updates
                            log.atInfo().setMessage("latestMatchUrl matches last checked").addKeyValue("player", player.getName()).log();
                            return null;
                        }
                    } else {
                        log.atInfo().setMessage(player.getName() + " - CheckedStatus is null").addKeyValue("player", player.getName()).log();
                        return null;
                    }
                }

                String homeTeam, awayTeam, relevantTeam;
                if (HOME.equals(resultRow.select("td[data-stat=venue]").text())) {
                    homeTeam = resultRow.select("td[data-stat=team] > a").text();
                    awayTeam = resultRow.select("td[data-stat=opponent] > a").text();
                    relevantTeam = homeTeam;
                } else {
                    homeTeam = resultRow.select("td[data-stat=opponent] > a").text();
                    awayTeam = resultRow.select("td[data-stat=team] > a").text();
                    relevantTeam = awayTeam;
                }

                PlayerMatchPerformanceStats playerMatchPerformanceStats = PlayerMatchPerformanceStats.builder()
                        .dataSourceSiteName(dataSourceSiteName)
                        .match(new Match(latestMatchUrl, selectedMatchDate, homeTeam, awayTeam, relevantTeam))
                        .minutesPlayed(parseIntegerOrNull(resultRow.select("td[data-stat=minutes]").text()))
                        .goals(parseIntegerOrNull(resultRow.select("td[data-stat=goals]").text()))
                        .assists(parseIntegerOrNull(resultRow.select("td[data-stat=assists]").text()))
                        .penaltiesScored(parseIntegerOrNull(resultRow.select("td[data-stat=pens_made]").text()))
                        .penaltiesWon(parseIntegerOrNull(resultRow.select("td[data-stat=pens_won]").text()))
                        .shots(parseIntegerOrNull(resultRow.select("td[data-stat=shots]").text()))
                        .shotsOnTarget(parseIntegerOrNull(resultRow.select("td[data-stat=shots_on_target]").text()))
                        .yellowCards(parseIntegerOrNull(resultRow.select("td[data-stat=cards_yellow]").text()))
                        .redCards(parseIntegerOrNull(resultRow.select("td[data-stat=cards_red]").text()))
                        .fouls(parseIntegerOrNull(resultRow.select("td[data-stat=fouls]").text()))
                        .fouled(parseIntegerOrNull(resultRow.select("td[data-stat=fouled]").text()))
                        .offsides(parseIntegerOrNull(resultRow.select("td[data-stat=offsides]").text()))
                        .crosses(parseIntegerOrNull(resultRow.select("td[data-stat=crosses]").text()))
                        .touches(parseIntegerOrNull(resultRow.select("td[data-stat=touches]").text()))
                        .tackles(parseIntegerOrNull(resultRow.select("td[data-stat=tackles]").text()))
                        .tacklesWon(parseIntegerOrNull(resultRow.select("td[data-stat=tackles_won]").text()))
                        .interceptions(parseIntegerOrNull(resultRow.select("td[data-stat=interceptions]").text()))
                        .blocks(parseIntegerOrNull(resultRow.select("td[data-stat=blocks]").text()))
                        .xg(parseFloatOrNull(resultRow.select("td[data-stat=xg]").text()))
                        .xg_assist(parseFloatOrNull(resultRow.select("td[data-stat=xg_assist]").text()))
                        .shotCreatingActions(parseIntegerOrNull(resultRow.select("td[data-stat=sca]").text()))
                        .goalCreatingActions(parseIntegerOrNull(resultRow.select("td[data-stat=gca]").text()))
                        .passesCompleted(parseIntegerOrNull(resultRow.select("td[data-stat=passes_completed]").text()))
                        .passesAttempted(parseIntegerOrNull(resultRow.select("td[data-stat=passes]").text()))
                        .progressivePasses(parseIntegerOrNull(resultRow.select("td[data-stat=progressive_passes]").text()))
                        .carries(parseIntegerOrNull(resultRow.select("td[data-stat=carries]").text()))
                        .progressiveCarries(parseIntegerOrNull(resultRow.select("td[data-stat=progressive_carries]").text()))
                        .takesOnsAttempted(parseIntegerOrNull(resultRow.select("td[data-stat=take_ons]").text()))
                        .takesOnsCompleted(parseIntegerOrNull(resultRow.select("td[data-stat=take_ons_won]").text()))
                        .gkShotsOnTargetAgainst(parseIntegerOrNull(resultRow.select("td[data-stat=gk_shots_on_target_against]").text()))
                        .gkGoalsAgainst(parseIntegerOrNull(resultRow.select("td[data-stat=gk_goals_against]").text()))
                        .gkSaves(parseIntegerOrNull(resultRow.select("td[data-stat=gk_saves]").text()))
                        .gkSavePercentage(parseFloatOrNull(resultRow.select("td[data-stat=gk_save_pct]").text()))
                        .gkPenaltiesAttemptedAgainst(parseIntegerOrNull(resultRow.select("td[data-stat=gk_pens_att]").text()))
                        .gkPenaltiesScoredAgainst(parseIntegerOrNull(resultRow.select("td[data-stat=gk_pens_allowed]").text()))
                        .gkPenaltiesSaved(parseIntegerOrNull(resultRow.select("td[data-stat=gk_pens_saved]").text()))
                        .build();

                StatHelper.populateStatPercentages(playerMatchPerformanceStats);
                return playerMatchPerformanceStats;
            }
            log.atInfo().setMessage("Unable to update player, checked all games").addKeyValue("player", player.getName()).log();
        } catch (Exception ex) {
            log.warn("Error while trying to update player: " + player.getName() + " - " + ex);
        }
        return null;
    }

    @Override
    public void parseSquadDataForTeam(Team team, DataSource dataSource, List<Player> players) {
        try {
            Document doc = Jsoup.connect(dataSource.getUrl()).get();

            Element tableElement = doc.getElementById("roster");
            if (tableElement == null) {
                log.info("Unable to find roster/squad: table");
                return;
            }
            Element tbodyElement = tableElement.getElementsByTag("tbody").first();
            if (tbodyElement == null) {
                log.info("Cannot find any match results: tbody");
                return;
            }
            Elements playerRows = tbodyElement.select("tr");
            if (playerRows.isEmpty()) {
                log.info("Cannot find any match results in table: tr");
                return;
            }
            for (Element playerRow : playerRows) {
                String playerName = playerRow.select("td[data-stat=player] > a").text();
                DataSource newDataSource = new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FBREF, generatePlayerUrl(playerRow, playerName));
                // Player exists in passed player list
                if (players.stream().anyMatch(o -> o.getName().equals(playerName))) {
                    Player existingPlayer = players.stream().filter(o -> o.getName().equals(playerName)).findFirst().get();
                    existingPlayer.getDataSources().add(newDataSource);
                } else {
                    // Create player and add to list
                    Player player = new Player(
                            playerName,
                            dateFormat.parse(playerRow.select("td[data-stat=birth_date]").text())
                    );
                    Set<DataSource> dataSources = new HashSet<>();
                    dataSources.add(newDataSource);
                    player.setDataSources(dataSources);
                    players.add(player);
                }
            }
        } catch (Exception ex) {
            log.warn("Unable to create players list to add: " + ex);
        }
    }

    private String generatePlayerUrl(Element playerRow, String playerName) throws IllegalArgumentException {
        // Example url
        // https://fbref.com/en/players/30f6344f/matchlogs/2022-2023/Mackenzie-Arnold-Match-Logs
        String input = playerRow.select("td[data-stat=player] > a").attr("href");
        Pattern pattern = Pattern.compile(PATH_REGEX);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String playerNameUrl = playerName.replaceAll(" ", "-");
            return BASEURL + matcher.group() + PATH_SUFFIX + "/" + playerNameUrl + "-Match-Logs";
        } else {
            throw new IllegalArgumentException("Unable to generate player url due to regex not matching for player: " + playerName);
        }
    }
}
