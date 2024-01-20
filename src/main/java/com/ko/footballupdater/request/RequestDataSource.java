package com.ko.footballupdater.request;


import com.ko.footballupdater.models.DataSourceSiteName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestDataSource {

    private DataSourceSiteName siteName;
    private String url;

    public RequestDataSource() {
    }

    public RequestDataSource(DataSourceSiteName siteName, String url) {
        this.siteName = siteName;
        this.url = url;
    }
}
