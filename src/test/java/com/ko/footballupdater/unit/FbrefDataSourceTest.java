package com.ko.footballupdater.unit;

import com.ko.footballupdater.datasource.FbrefDataSource;
import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.DataSourceType;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class FbrefDataSourceTest {

    @InjectMocks
    private FbrefDataSource fbrefDataSource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testParsePlayerMatchData_success() throws IOException {
        Player player = new Player("name", new Date());
        player.setCheckedStatus(new CheckedStatus(DataSourceSiteName.FBREF));
        String htmlContent = readHtmlFile("src/test/java/com/ko/footballupdater/unit/sample/FbrefPlayerMatchDataTest.html");
        Document doc = Jsoup.parse(htmlContent);

        PlayerMatchPerformanceStats result = fbrefDataSource.parsePlayerMatchData(player, doc);
        assertNotNull(result);
        assertEquals("/en/matches/2d94f108/Arsenal-Paris-FC-September-9-2023-Champions-League", result.getMatch().getUrl());
        assertEquals("Paris FC", result.getMatch().getHomeTeamName());
        assertEquals("Arsenal", result.getMatch().getAwayTeamName());
        assertEquals("Arsenal", result.getMatch().getRelevantTeam());
        assertEquals(58, result.getMinutesPlayed());
        assertEquals(0, result.getGoals());
        assertEquals(0, result.getAssists());
    }

//    @Test
//    public void testParseSquadDataForTeam() {
//        // Implement your test for parseSquadDataForTeam here
//        // Mock dependencies and verify the expected behavior
//
//        // Example:
//        Team team = new Team(/* create a Team instance here */);
//        DataSource dataSource = new DataSource(/* create a DataSource instance here */);
//
//        // Mock external dependencies or expectations
//
//        // Call the method to be tested
//        List<Player> result = fbrefDataSource.parseSquadDataForTeam(team, dataSource);
//
//        // Assert the expected behavior or result
//        assertNotNull(result); // Add more assertions based on your test requirements
//    }

    private String readHtmlFile(String fileName) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String str;
        while ((str = in.readLine()) != null) {
            contentBuilder.append(str);
        }
        in.close();

        return contentBuilder.toString();
    }
}
