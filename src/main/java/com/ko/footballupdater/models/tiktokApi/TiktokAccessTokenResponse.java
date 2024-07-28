package com.ko.footballupdater.models.tiktokApi;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TiktokAccessTokenResponse {

    private String open_id;
    private String scope;
    private String access_token;
    private String expires_in;
    private String refresh_token;
    private String refresh_expires_in;
    private String token_type;

}
