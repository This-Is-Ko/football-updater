package com.ko.footballupdater.services;

import com.ko.footballupdater.datasource.DataSourceParser;
import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Team;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
public class ParsingService {

    @Autowired
    private List<DataSourceParser> dataSourceParsers;

    @NotNull
    @Value("#{'${datasource.priority}'.split(',')}")
    private List<DataSourceSiteName> dataSourcePriority;

    @NotNull
    @Value("${datasource.sitename}")
    private DataSourceSiteName dataSourceSiteName;

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
        // Use data source based on config order
        // If the data source does not resolve a new match, try the next data source using match date to compare
        List<DataSource> dataSources = new ArrayList<>();
        if (player.getDataSources() != null && !player.getDataSources().isEmpty()) {
            player.getDataSources().iterator().forEachRemaining(dataSources::add);
        }

        for (DataSourceSiteName source : dataSourcePriority) {
            if (dataSources.stream().anyMatch(o -> o.getSiteName().equals(source))) {
                DataSource dataSource = dataSources.stream().filter(o -> o.getSiteName().equals(source)).findFirst().get();

                log.atInfo().setMessage("Attempting dataSource " + dataSource.getSiteName()).addKeyValue("player", player.getName()).log();

                for (DataSourceParser dataSourceParser : dataSourceParsers) {
                    if (dataSourceParser.getDataSourceSiteName().equals(dataSource.getSiteName())) {
                        try {
                            Document doc = Jsoup.connect(dataSource.getUrl()).get();
                            PlayerMatchPerformanceStats playerMatchPerformanceStats = dataSourceParser.parsePlayerMatchData(player, doc);
                            if (playerMatchPerformanceStats != null) {
                                log.atInfo().setMessage(dataSource.getSiteName() + " - Successfully parse player data").addKeyValue("player", player.getName()).log();
                                player.getCheckedStatus().setSiteName(dataSource.getSiteName());
                                return playerMatchPerformanceStats;
                            }
                        } catch (IOException ex) {
                            log.atWarn().setMessage("Unable to retrieve page at " + dataSource.getUrl()).setCause(ex).addKeyValue("player", player.getName()).log();
                            return null;
                        }
                        break;
                    }
                }
            }
        }
        return null;
    }

    public List<Player> parseSquadDataForTeam(Team team) {
        if (team.getDataSources() != null && !team.getDataSources().isEmpty()) {
            Iterator<DataSource> dataSourceIterator = team.getDataSources().iterator();

            List<Player> players = new ArrayList<>();
            while (dataSourceIterator.hasNext()) {
                DataSource dataSource = dataSourceIterator.next();
                for (DataSourceParser dataSourceParser : dataSourceParsers) {
                    if (dataSourceParser.getDataSourceSiteName().equals(dataSource.getSiteName())) {
                        dataSourceParser.parseSquadDataForTeam(team, dataSource, players);
                    }
                }
            }
            return players;
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
