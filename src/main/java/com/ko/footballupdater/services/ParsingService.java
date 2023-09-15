package com.ko.footballupdater.services;

import com.ko.footballupdater.datasource.DataSourceParser;
import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Team;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class ParsingService {

    @Autowired
    private List<DataSourceParser> dataSourceParsers;

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
                if (!dataSource.getSiteName().equals(player.getCheckedStatus().getSiteName())) {
                    log.info("Last checked was " + player.getCheckedStatus().getSiteName() + "; dataSource is " + dataSource.getSiteName());
                }
                for (DataSourceParser dataSourceParser : dataSourceParsers) {
                    if (dataSourceParser.getDataSourceSiteName().equals(dataSource.getSiteName())) {
                        try {
                            Document doc = Jsoup.connect(dataSource.getUrl()).get();
                            return dataSourceParser.parsePlayerMatchData(player, doc);
                        } catch (IOException e) {
                            log.warn("Unable to retrieve page at " + dataSource.getUrl());
                            return null;
                        }
                    }
                }
            }

        }
        return null;
    }

    public List<Player> parseSquadDataForTeam(Team team) {
        if (team.getDataSources() != null && !team.getDataSources().isEmpty()) {
            while (team.getDataSources().iterator().hasNext()) {
                DataSource dataSource = team.getDataSources().iterator().next();
                for (DataSourceParser dataSourceParser : dataSourceParsers) {
                    if (dataSourceParser.getDataSourceSiteName().equals(dataSource.getSiteName())) {
                        return dataSourceParser.parseSquadDataForTeam(team, dataSource);
                    }
                }
            }

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
