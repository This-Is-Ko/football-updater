package com.ko.footballupdater.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tiktok.api")
public class TiktokApiProperties {

    @NotNull
    private String clientKey;

    @NotNull
    private String clientSecret;

    @NotNull
    private String scope;

    @NotNull
    private String redirectUri;

}
