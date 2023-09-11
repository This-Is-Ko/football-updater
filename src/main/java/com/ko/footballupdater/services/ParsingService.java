package com.ko.footballupdater.services;

import com.ko.footballupdater.models.DataSource;
import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.Team;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ParsingService {

    @Value("${datasource.sitename}")
    private DataSourceSiteName dataSourceSiteName;

    public void parseTeamData(Team team) {
        if (team.getDataSources() != null && !team.getDataSources().isEmpty()){
            while(team.getDataSources().iterator().hasNext()){
                DataSource dataSource = team.getDataSources().iterator().next();
                try{
                    if (dataSourceSiteName.equals(dataSource.getSiteName())){
                        Document doc = Jsoup.connect(dataSource.getUrl()).get();

                        // Potentially support other sites
                        switch (dataSourceSiteName) {
                            case FBREF -> {
                                Element tableElement = doc.getElementsByClass("stats_table").first();
                                if (tableElement != null) {
                                    Element tbodyElement = tableElement.getElementsByTag("tbody").first();
                                    if (tbodyElement != null) {
                                        Elements resultRows = tbodyElement.select("tr");
                                        while(resultRows.iterator().hasNext()) {
                                            Element resultRow = resultRows.iterator().next();
                                            resultRow.select("td[data-stat=match_report]");

                                            if (team.getUpdateStatus().getSiteName().equals(dataSourceSiteName)) {
                                                // Compare to previous if source is the same

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
    }
}
