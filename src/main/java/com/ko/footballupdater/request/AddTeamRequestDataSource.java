package com.ko.footballupdater.request;


import com.ko.footballupdater.models.DataSourceSiteName;
import com.ko.footballupdater.models.DataSourceType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTeamRequestDataSource {

    private DataSourceType type;
    private DataSourceSiteName siteName;
    private String url;

}
