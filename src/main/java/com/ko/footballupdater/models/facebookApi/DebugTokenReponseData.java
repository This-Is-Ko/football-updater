package com.ko.footballupdater.models.facebookApi;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DebugTokenReponseData {

    private String app_id;
    private String type;
    private String application;
    private List<String> scopes;
    private String user_id;

    public DebugTokenReponseData() {
    }
}
