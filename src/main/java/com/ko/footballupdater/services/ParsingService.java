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
import org.jsoup.HttpStatusException;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ParsingService {

    @Autowired
    private List<DataSourceParser> dataSourceParsers;

    @NotNull
    @Value("#{'${datasource.priority}'.split(',')}")
    private List<DataSourceSiteName> dataSourcePriority;

    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player) throws InterruptedException {
        return parsePlayerMatchData(player, false);
    }

    public PlayerMatchPerformanceStats parsePlayerMatchData(Player player, boolean skipLatestMatchCheck) throws InterruptedException {
        // Use data source based on config order
        // If the data source does not resolve a new match, try the next data source using match date to compare
        List<DataSource> dataSources = new ArrayList<>();
        if (player.getDataSources() != null && !player.getDataSources().isEmpty()) {
            player.getDataSources().iterator().forEachRemaining(dataSources::add);
        }

        for (DataSourceSiteName source : dataSourcePriority) {
            if (dataSources.stream().anyMatch(o -> o.getSiteName().equals(source))) {
                DataSource dataSource = dataSources.stream().filter(o -> o.getSiteName().equals(source)).findFirst().get();

                log.atInfo().setMessage(dataSource.getSiteName() + " - Attempting dataSource").addKeyValue("player", player.getName()).log();

                for (DataSourceParser dataSourceParser : dataSourceParsers) {
                    if (dataSourceParser.getDataSourceSiteName().equals(dataSource.getSiteName())) {
                        try {
                            Document doc = Jsoup.connect(dataSource.getUrl()).ignoreContentType(true).get();
                            PlayerMatchPerformanceStats playerMatchPerformanceStats = dataSourceParser.parsePlayerMatchData(player, doc, dataSource.getUrl(), skipLatestMatchCheck);
                            if (playerMatchPerformanceStats != null) {
                                log.atInfo().setMessage(dataSource.getSiteName() + " - Successfully parse player data").addKeyValue("player", player.getName()).log();
                                player.getCheckedStatus().setSiteName(dataSource.getSiteName());
                                return playerMatchPerformanceStats;
                            } else {
                                log.atInfo().setMessage(dataSource.getSiteName() + " - Unable to construct stats object").addKeyValue("player", player.getName()).log();
                            }
                        } catch (HttpStatusException ex) {
                            log.atWarn().setMessage(dataSource.getSiteName() + " - Status=" + ex.getStatusCode() + " - Unable to retrieve page at " + dataSource.getUrl()).addKeyValue("player", player.getName()).log();
                            if (ex.getStatusCode() == 429) {
                                // Sleep for longer
                                log.atInfo().setMessage("Added wait time of 20 sec").addKeyValue("player", player.getName()).log();
                                TimeUnit.SECONDS.sleep(20);
                            }
                            return null;
                        } catch (IOException ex) {
                            log.atWarn().setMessage(dataSource.getSiteName() + " - Unable to retrieve page at " + dataSource.getUrl()).setCause(ex).addKeyValue("player", player.getName()).log();
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
}
