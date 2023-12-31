package com.ko.footballupdater.datasource;

import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Team;
import org.jsoup.nodes.Document;

import java.util.List;

public interface DataSourceParser {

    DataSourceSiteName getDataSourceSiteName();

    PlayerMatchPerformanceStats parsePlayerMatchData(Player player, Document document);

    PlayerMatchPerformanceStats parsePlayerMatchData(Player player, Document document, String url, boolean skipLatestMatchCheck);

    void parseSquadDataForTeam(Team team, DataSource dataSource, List<Player> players);

}
