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

    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player) {
        return parsePlayerMatchData(player, false);
    }

    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player, boolean skipLatestMatchCheck) {
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
                            Document doc = Jsoup.connect(dataSource.getUrl()).ignoreContentType(true).get();
                            PlayerMatchPerformanceStats playerMatchPerformanceStats = dataSourceParser.parsePlayerMatchData(player, doc, dataSource.getUrl(), skipLatestMatchCheck);
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
        log.atInfo().setMessage("Finished checking data sources").addKeyValue("player", player.getName()).log();
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
