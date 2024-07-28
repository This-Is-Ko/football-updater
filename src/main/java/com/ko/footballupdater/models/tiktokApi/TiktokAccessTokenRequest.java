package com.ko.footballupdater.models.tiktokApi;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TiktokAccessTokenRequest {

    private String client_key;
    private String client_secret;
    private String code;
    private String grant_type;
    private String redirect_uri;
}
