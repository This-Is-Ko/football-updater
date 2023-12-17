package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.datasource.DataSourceParser;
import com.ko.footballupdater.datasource.FbrefDataSource;
import com.ko.footballupdater.datasource.FotmobDataSource;
import com.ko.footballupdater.models.CheckedStatus;
import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.DataSourceType;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.services.ParsingService;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ParsingServiceTest {

    @InjectMocks
    private ParsingService parsingService;

    private DataSourceParser fbrefDataSource = mock(FbrefDataSource.class);

    private DataSourceParser fotmobDataSource = mock(FotmobDataSource.class);

    private List<DataSourceParser> dataSourceParsers = new ArrayList<>(){{
        add(fbrefDataSource);
        add(fotmobDataSource);
    }};

    private List<DataSourceSiteName> dataSourcePriority = new ArrayList<>(){{
        add(DataSourceSiteName.FOTMOB);
        add(DataSourceSiteName.FBREF);
    }};

    private MockedStatic<Jsoup> jsoupMockedStatic = mockStatic(Jsoup.class);

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(parsingService, "dataSourceParsers", dataSourceParsers);
        ReflectionTestUtils.setField(parsingService, "dataSourcePriority", dataSourcePriority);

        Connection connection = mock(Connection.class);
        when(Jsoup.connect(anyString())).thenReturn(connection);
        when(connection.ignoreContentType(anyBoolean())).thenReturn(connection);
        when(connection.get()).thenReturn(new Document("url"));

        when(fotmobDataSource.parsePlayerMatchData(any(Player.class), any(Document.class), anyString(), anyBoolean())).thenReturn(new PlayerMatchPerformanceStats(DataSourceSiteName.FOTMOB));
        when(fotmobDataSource.getDataSourceSiteName()).thenReturn(DataSourceSiteName.FOTMOB);
        when(fbrefDataSource.parsePlayerMatchData(any(Player.class), any(Document.class), anyString(), anyBoolean())).thenReturn(new PlayerMatchPerformanceStats(DataSourceSiteName.FBREF));
        when(fbrefDataSource.getDataSourceSiteName()).thenReturn(DataSourceSiteName.FBREF);
    }

    @AfterEach
    void after() {
        jsoupMockedStatic.close();
    }

    @Test
    void testParsePlayerMatchData_MultipleDataSourcesAvailable_Successful() {
        // Priority FOTMOB, FBREF
        // return FOTMOB successful
        Set<DataSource> dataSources = new HashSet<>();
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FBREF, "url"));
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FOTMOB, "url"));
        Player testPlayer = new Player(0, "Test Player Name", null, new Date(), null, dataSources, new CheckedStatus(0, DataSourceSiteName.FBREF));

        PlayerMatchPerformanceStats result = parsingService.parsePlayerMatchData(testPlayer);

        assertNotNull(result);
        assertEquals(DataSourceSiteName.FOTMOB, result.getDataSourceSiteName());
    }

    @Test
    void testParsePlayerMatchData_MultipleDataSourcesAvailableForPlayer_DifferentOrder_Successful() {
        // Priority FOTMOB, FBREF
        // User datasource order is FBREF, FOTMOB
        // return FOTMOB successful
        Set<DataSource> dataSources = new HashSet<>();
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FOTMOB, "url"));
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FBREF, "url"));
        Player testPlayer = new Player(0, "Test Player Name", null, new Date(), null, dataSources, new CheckedStatus(0, DataSourceSiteName.FBREF));

        PlayerMatchPerformanceStats result = parsingService.parsePlayerMatchData(testPlayer);

        assertNotNull(result);
        assertEquals(DataSourceSiteName.FOTMOB, result.getDataSourceSiteName());
    }


    @Test
    void testParsePlayerMatchData_MultipleDataSourcesAvailable_DifferentPriorityOrder_Successful() {
        dataSourcePriority = new ArrayList<>(){{
            add(DataSourceSiteName.FBREF);
            add(DataSourceSiteName.FOTMOB);
        }};
        ReflectionTestUtils.setField(parsingService, "dataSourcePriority", dataSourcePriority);
        // Priority FBREF, FOTMOB
        // return FBREF successful
        Set<DataSource> dataSources = new HashSet<>();
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FBREF, "url"));
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FOTMOB, "url"));
        Player testPlayer = new Player(0, "Test Player Name", null, new Date(), null, dataSources, new CheckedStatus(0, DataSourceSiteName.FBREF));

        PlayerMatchPerformanceStats result = parsingService.parsePlayerMatchData(testPlayer);

        assertNotNull(result);
        assertEquals(DataSourceSiteName.FBREF, result.getDataSourceSiteName());
    }

    @Test
    void testParsePlayerMatchData_OneDataSourceAvailable_FOTMOB_Successful() {
        // Priority FOTMOB, FBREF
        // User datasource order is FOTMOB
        // return FOTMOB successful
        Set<DataSource> dataSources = new HashSet<>();
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FOTMOB, "url"));
        Player testPlayer = new Player(0, "Test Player Name", null, new Date(), null, dataSources, new CheckedStatus(0, DataSourceSiteName.FBREF));

        PlayerMatchPerformanceStats result = parsingService.parsePlayerMatchData(testPlayer);

        assertNotNull(result);
        assertEquals(DataSourceSiteName.FOTMOB, result.getDataSourceSiteName());
    }

    @Test
    void testParsePlayerMatchData_OneDataSourcesAvailable_FBREF_Successful() {
        // Priority FOTMOB, FBREF
        // User datasource order is FBREF
        // return FBREF successful
        Set<DataSource> dataSources = new HashSet<>();
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FBREF, "url"));
        Player testPlayer = new Player(0, "Test Player Name", null, new Date(), null, dataSources, new CheckedStatus(0, DataSourceSiteName.FBREF));

        PlayerMatchPerformanceStats result = parsingService.parsePlayerMatchData(testPlayer);

        assertNotNull(result);
        assertEquals(DataSourceSiteName.FBREF, result.getDataSourceSiteName());
    }

    @Test
    void testParsePlayerMatchData_MultipleDataSourcesAvailable_PriorityFails_Fallback_Successful() {
        when(fotmobDataSource.parsePlayerMatchData(any(Player.class), any(Document.class), anyString(), anyBoolean())).thenReturn(null);
        // Priority FOTMOB, FBREF
        // FOTMOB failes, fallback to FBREF
        // return FOTMOB successful
        Set<DataSource> dataSources = new HashSet<>();
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FBREF, "url"));
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FOTMOB, "url"));
        Player testPlayer = new Player(0, "Test Player Name", null, new Date(), null, dataSources, new CheckedStatus(0, DataSourceSiteName.FBREF));

        PlayerMatchPerformanceStats result = parsingService.parsePlayerMatchData(testPlayer);

        assertNotNull(result);
        assertEquals(DataSourceSiteName.FBREF, result.getDataSourceSiteName());
    }

    @Test
    void testParsePlayerMatchData_NoDataSource_Unsuccessful() {
        // Priority FOTMOB, FBREF
        // return null
        Set<DataSource> dataSources = new HashSet<>();
        Player testPlayer = new Player(0, "Test Player Name", null, new Date(), null, dataSources, new CheckedStatus(0, DataSourceSiteName.FBREF));

        PlayerMatchPerformanceStats result = parsingService.parsePlayerMatchData(testPlayer);

        assertNull(result);
    }

    @Test
    void testParsePlayerMatchData_MultipleDataSourcesAvailable_AllDataSourcesFail_Unsuccessful() {
        when(fotmobDataSource.parsePlayerMatchData(any(Player.class), any(Document.class), anyString(), anyBoolean())).thenReturn(null);
        when(fbrefDataSource.parsePlayerMatchData(any(Player.class), any(Document.class), anyString(), anyBoolean())).thenReturn(null);
        // Priority FOTMOB, FBREF
        // FOTMOB failes, fallback to FBREF also fails
        // return null
        Set<DataSource> dataSources = new HashSet<>();
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FBREF, "url"));
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FOTMOB, "url"));
        Player testPlayer = new Player(0, "Test Player Name", null, new Date(), null, dataSources, new CheckedStatus(0, DataSourceSiteName.FBREF));

        PlayerMatchPerformanceStats result = parsingService.parsePlayerMatchData(testPlayer);

        assertNull(result);
    }

    @Test
    void testParsePlayerMatchData_ErrorFromJsoupConnect_AllDataSources_Unsuccessful() throws IOException {
        Connection newConnection = mock(Connection.class);
        when(Jsoup.connect(anyString())).thenReturn(newConnection);
        when(newConnection.ignoreContentType(anyBoolean())).thenReturn(newConnection);
        when(newConnection.get()).thenThrow(new IOException("error"));

        Set<DataSource> dataSources = new HashSet<>();
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FBREF, "FBREF url"));
        dataSources.add(new DataSource(DataSourceType.PLAYER, DataSourceSiteName.FOTMOB, "FOTMOB url"));
        Player testPlayer = new Player(0, "Test Player Name", null, new Date(), null, dataSources, new CheckedStatus(0, DataSourceSiteName.FBREF));

        PlayerMatchPerformanceStats result = parsingService.parsePlayerMatchData(testPlayer);

        assertNull(result);
    }

}
