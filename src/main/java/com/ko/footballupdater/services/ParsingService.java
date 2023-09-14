package com.ko.footballupdater.services;

import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Team;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;

@Service
public class ParsingService {

    Logger LOG = LoggerFactory.getLogger(ParsingService.class);

    @Value("${datasource.sitename}")
    private DataSourceSiteName dataSourceSiteName;

    private final String HOME = "Home";

    public String parseTeamData(Team team) {
        if (team.getDataSources() != null && !team.getDataSources().isEmpty()) {
            while (team.getDataSources().iterator().hasNext()) {
                DataSource dataSource = team.getDataSources().iterator().next();
                try {
                    if (dataSourceSiteName.equals(dataSource.getSiteName())) {
                        Document doc = Jsoup.connect(dataSource.getUrl()).get();

                        // Potentially support other sites
                        // TODO Change to interface
                        switch (dataSourceSiteName) {
                            case FBREF -> {
                                Element tableElement = doc.getElementsByClass("stats_table").first();
                                if (tableElement != null) {
                                    Element tbodyElement = tableElement.getElementsByTag("tbody").first();
                                    if (tbodyElement != null) {
                                        Elements resultRows = tbodyElement.select("tr");
                                        boolean useNext = false;
                                        if (resultRows.isEmpty()) {
                                            break;
                                        }
                                        for (Element resultRow : resultRows) {
                                            String matchUrl = resultRow.select("th[data-stat=date] > a").attr("href");

                                            if (team.getCheckedStatus() == null) {
                                                team.setCheckedStatus(new CheckedStatus(dataSourceSiteName));
                                            }

                                            if (team.getCheckedStatus().getSiteName().equals(dataSourceSiteName)) {
                                                // Compare to previous if source is the same
                                                String previousData = team.getCheckedStatus().getLatestCheckedMatchUrl();
                                                if (useNext || previousData == null) {
                                                    // Use current matchUrl
                                                    return matchUrl;
                                                }
                                                if (previousData.equals(matchUrl)) {
                                                    // Matches previous match link therefore next match will be the latest unchecked
                                                    useNext = true;
                                                }
                                            } else {
                                                // Previous source is not the same
                                                throw new IllegalStateException("Unexpected value: " + team.getCheckedStatus().getLatestCheckedMatchUrl() + "; Expected: " + dataSourceSiteName);
                                            }
                                        }
                                    }
                                }
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + dataSourceSiteName);
                        }
                        break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        return null;
    }

    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player) {
        if (player.getDataSources() != null && !player.getDataSources().isEmpty()) {
            while (player.getDataSources().iterator().hasNext()) {
                DataSource dataSource = player.getDataSources().iterator().next();
                try {
                    if (dataSourceSiteName.equals(dataSource.getSiteName())) {
                        Document doc = Jsoup.connect(dataSource.getUrl()).get();

                        // Potentially support other sites
                        // TODO Change to interface
                        switch (dataSourceSiteName) {
                            case FBREF -> {
                                Element tableElement = doc.getElementsByClass("stats_table").first();
                                if (tableElement != null) {
                                    Element tbodyElement = tableElement.getElementsByTag("tbody").first();
                                    if (tbodyElement != null) {
                                        Elements resultRows = tbodyElement.select("tr");
                                        if (resultRows.isEmpty()) {
                                            break;
                                        }
                                        // Assume last row is the latest match
                                        Collections.reverse(resultRows);
                                        for (Element resultRow : resultRows) {
                                            // For games not played, appears as unused_sub class
                                            if (resultRow.getElementsByClass("unused_sub").isEmpty()) {
                                                String latestMatchUrl = resultRow.select("th[data-stat=date] > a").attr("href");

                                                // Check if match is new
                                                if (player.getCheckedStatus() == null || (player.getCheckedStatus().getLatestCheckedMatchUrl() != null && player.getCheckedStatus().getLatestCheckedMatchUrl().equals(latestMatchUrl))) {
                                                    // No new updates
                                                    LOG.info("latestMatchUrl matches last checked");
                                                    return null;
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
                                                return new PlayerMatchPerformanceStats(
                                                        new Match(latestMatchUrl, homeTeam, awayTeam, relevantTeam),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=minutes]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=goals]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=assists]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=pens_made]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=pens_won]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=shots]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=shots_on_target]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=cards_yellow]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=cards_red]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=fouls]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=fouled]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=offsides]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=crosses]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=touches]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=tackles]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=tackles_won]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=interceptions]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=blocks]").text()),
                                                        parseFloatOrNull(resultRow.select("td[data-stat=xg]").text()),
                                                        parseFloatOrNull(resultRow.select("td[data-stat=xg_assist]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=sca]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=gca]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=passes_completed]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=passes]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=passes_pct]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=progressive_passes]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=carries]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=progressive_carries]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=take_ons]").text()),
                                                        parseIntegerOrNull(resultRow.select("td[data-stat=take_ons_won]").text())
                                                );
                                            }
                                        }
                                    }
                                }
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + dataSourceSiteName);
                        }
                        break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        return null;
    }

    private Integer parseIntegerOrNull(String input) {
        if (input != null && !input.isEmpty()) {
            return Integer.parseInt(input);
        }
        return null;
    }
    private Float parseFloatOrNull(String input) {
        if (input != null && !input.isEmpty()) {
            return Float.parseFloat(input);
        }
        return null;
    }

//    public String parseMatchDataForTeam(Team team, List<Player> players, String matchLink) {
//        try {
//            Document doc = Jsoup.connect(matchLink).get();
//
//            List<String> lineupNames = new ArrayList<>();
//            List<Player> startingLineup = new ArrayList<>();
//            List<Player> benchLineup = new ArrayList<>();
//            // Potentially support other sites
//            switch (dataSourceSiteName) {
//                case FBREF -> {
//                    Elements lineups = doc.select("div.lineup");
//                    // Expect 2 lineup elements
//                    if (lineups.size() != 2) {
//                        return null;
//                    }
//                    for (Element lineup : lineups) {
//                        String lineupTeamName = lineup.select("table > tbody > tr > th").text();
//                        // Check lineup matches team
//                        if (lineupTeamName.contains(team.getName())) {
//                            // Get all players in lineup
//                            Elements lineupRows = lineup.select("tr");
//                            for (Element lineupRow : lineupRows) {
//                                String playerName = lineupRow.select("td > a").text();
//                                if (!playerName.isEmpty()){
//                                    lineupNames.add(playerName);
//                                }
//                            }
//                        }
//                    }
//
//                    // Find wanted players
//
//                }
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
