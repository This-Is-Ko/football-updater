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
    // Unix timestamp (seconds) when the token expires, 0 means it doesn't expire
    private Long expires_at;
    private Boolean is_valid;

    public DebugTokenReponseData() {
    }
}
