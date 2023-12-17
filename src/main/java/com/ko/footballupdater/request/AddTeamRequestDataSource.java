package com.ko.footballupdater.request;


import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.DataSourceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTeamRequestDataSource {

    private DataSourceSiteName siteName;
    private String url;

    public AddTeamRequestDataSource() {
    }

    public AddTeamRequestDataSource( DataSourceSiteName siteName, String url) {
        this.siteName = siteName;
        this.url = url;
    }
}
