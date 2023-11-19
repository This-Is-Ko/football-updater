package com.ko.footballupdater.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "facebook.api")
public class FacebookApiProperties {

    @NotNull
    private String clientId;

    @NotNull
    private String clientSecret;

    @NotNull
    private String responseType;

    @NotNull
    private String scope;

    @NotNull
    private String redirectUri;

    @NotNull
    private FacebookApiInstagramProperties instagram;

}
