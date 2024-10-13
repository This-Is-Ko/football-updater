package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.TiktokApiProperties;
import com.ko.footballupdater.models.facebookApi.TiktokAccessTokenResponse;
import com.ko.footballupdater.models.form.TiktokApiDto;
import com.ko.footballupdater.utils.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Slf4j
@Service
public class TiktokApiService extends AbstractAuthClass {

    @Autowired
    TiktokApiProperties tiktokApiProperties;

    private static final String TIKTOK_AUTH_BASE_URL = "https://www.tiktok.com/v2";
    private static final String TIKTOK_API_BASE_URL = "https://open.tiktokapis.com/v2";

    private final WebClient tiktokWebClient;

    public TiktokApiService(WebClient.Builder tiktokWebClientBuilder) {
        this.tiktokWebClient = tiktokWebClientBuilder
                .baseUrl(TIKTOK_API_BASE_URL)
                .filter(logRequest())
                .filter(logResponse()).build();
    }

    public TiktokApiDto prepareTiktokApiDto() {
        TiktokApiDto tiktokApiDto = new TiktokApiDto();
        if (isTokenValid()) {
            tiktokApiDto.setCurrentlyLoggedIn(true);
        } else {
            tiktokApiDto.setCurrentlyLoggedIn(false);
            tiktokApiDto.setLoginUri(generateLoginString());
        }
        return tiktokApiDto;
    }

    public void handleLogin(String state, String code) throws Exception {
        verifyStateValue(state);
        // Exchange code for access token
        if (code != null && !code.isEmpty()) {
            TiktokAccessTokenResponse response = requestAccessToken(code);
            storeTokensInMemory(response);
        } else {
            throw new Exception("Login code param is empty");
        }
    }

    public TiktokAccessTokenResponse requestAccessToken(String code) {
        return tiktokWebClient.post()
                .uri("/oauth/token/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cache-Control", "no-cache")
                .body(BodyInserters.fromFormData("client_key", tiktokApiProperties.getClientKey())
                        .with("client_secret", tiktokApiProperties.getClientSecret())
                        .with("code", code)
                        .with("grant_type", "authorization_code")
                        .with("redirect_uri", tiktokApiProperties.getRedirectUri()))
                .retrieve()
                .bodyToMono(TiktokAccessTokenResponse.class)
                .block();
    }

    public String generateLoginString() {
        UUID uuid = UUID.randomUUID();
        state = uuid.toString();
        String codeVerifier = StringHelper.generateRandomString(43);
        String sha256EncodedCodeVerifier = StringHelper.encodeWithSHA256(codeVerifier);

        return TIKTOK_AUTH_BASE_URL +
                "/auth/authorize/" +
                "?client_key=" + tiktokApiProperties.getClientKey() +
                "&response_type=code" +
                "&scope=" + tiktokApiProperties.getScope() +
                "&redirect_uri=" + tiktokApiProperties.getRedirectUri() +
                "&state=" + state +
                "&code_challenge_method=S256" +
                "&code_challenge=" + sha256EncodedCodeVerifier;
    }
}
