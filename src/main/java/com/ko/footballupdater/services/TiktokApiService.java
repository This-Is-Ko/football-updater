package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.TiktokApiProperties;
import com.ko.footballupdater.models.facebookApi.FacebookAccessTokenResponse;
import com.ko.footballupdater.models.tiktokApi.TiktokAccessTokenRequest;
import com.ko.footballupdater.models.tiktokApi.TiktokAccessTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class TiktokApiService {

    @Autowired
    TiktokApiProperties tiktokApiProperties;

    private static final String TIKTOK_API_BASE_URL = "https://open.tiktokapis.com";
    private static final String TIKTOK_API_OAUTH_ENDPOINT = "/v2/oauth/token/";
    private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    private String accessToken;
    private final WebClient tiktokWebClient;
    public TiktokApiService(WebClient.Builder tiktokWebClientBuilder) {
        this.tiktokWebClient = tiktokWebClientBuilder.baseUrl(TIKTOK_API_BASE_URL).build();
    }

    private TiktokAccessTokenResponse exchangeCodeForAccessToken(String code) {
        // Request URL structure:
        // client_key
        // client_secret
        // code
        // grant_type
        // redirect_uri
        String apiUrl = String.format(TIKTOK_API_OAUTH_ENDPOINT);
        TiktokAccessTokenRequest request = TiktokAccessTokenRequest.builder()
                .client_key(tiktokApiProperties.getClientKey())
                .client_secret(tiktokApiProperties.getClientSecret())
                .code(code)
                .grant_type(AUTHORIZATION_CODE_GRANT_TYPE)
                .redirect_uri(tiktokApiProperties.getRedirectUri()).build();
        return tiktokWebClient.post()
                .uri(apiUrl)
                .body(Mono.just(request), TiktokAccessTokenRequest.class)
                .retrieve()
                .bodyToMono(TiktokAccessTokenResponse.class)
                .block();
    }

}
