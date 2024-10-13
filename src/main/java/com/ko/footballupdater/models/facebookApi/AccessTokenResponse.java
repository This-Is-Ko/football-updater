package com.ko.footballupdater.models.facebookApi;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AccessTokenResponse {

    private String access_token;
    private String refresh_token;
    private String token_type;
    private Integer expires_in;

}
