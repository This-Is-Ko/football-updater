package com.ko.footballupdater.datasource;

import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Team;

import java.util.List;

public interface DataSourceParser {

    DataSourceSiteName getDataSourceSiteName();

    PlayerMatchPerformanceStats parsePlayerMatchData(Player player, DataSource dataSource);

    List<Player> parseSquadDataForTeam(Team team, DataSource dataSource);

}
