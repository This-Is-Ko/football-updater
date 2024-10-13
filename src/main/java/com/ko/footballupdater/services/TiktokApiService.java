package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.TiktokApiProperties;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.models.tiktokApi.CreatorInfoResponse;
import com.ko.footballupdater.models.tiktokApi.PostPhotoRequest;
import com.ko.footballupdater.models.tiktokApi.PostPhotoRequestPostInfo;
import com.ko.footballupdater.models.tiktokApi.PostPhotoRequestSourceInfo;
import com.ko.footballupdater.models.tiktokApi.PostPhotoResponse;
import com.ko.footballupdater.models.tiktokApi.TiktokAccessTokenResponse;
import com.ko.footballupdater.models.form.TiktokApiDto;
import com.ko.footballupdater.utils.LogHelper;
import com.ko.footballupdater.utils.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        log.debug(String.format("state: %s, code: %s", state, code));
        verifyStateValue(state);
        // Exchange code for access token
        if (code != null && !code.isEmpty()) {
            TiktokAccessTokenResponse response = requestAccessToken(code);
            if (response != null) {
                if (response.getError() != null) {
                    throw new Exception(String.format("Error response from Tiktok auth: %s - %s", response.getError(), response.getErrorDescription()));
                } else {
                    storeTokensInMemory(response);
                    log.atInfo().setMessage("Tiktok login completed").log();
                }
            } else {
                throw new Exception("Response from Tiktok auth is empty");
            }
        } else {
            throw new Exception("Login code param is empty");
        }
    }

    public TiktokAccessTokenResponse requestAccessToken(String code) {
        String body = String.format("client_key=%s&client_secret=%s&code=%s&grant_type=authorization_code&redirect_uri=%s",
                tiktokApiProperties.getClientKey(),
                tiktokApiProperties.getClientSecret(),
                code,
                tiktokApiProperties.getRedirectUri());

        return tiktokWebClient.post()
                .uri("/oauth/token/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromValue(body))
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

    public void postToTiktok(Post post, List<ImageUrlEntry> imagesToUpload) throws Exception {
        LogHelper.logWithSubject(log.atInfo().setMessage("Attempting to post to Tiktok"), post);

        // Query Creator Info
        CreatorInfoResponse creatorInfoResponse = tiktokWebClient.post()
                .uri("/post/publish/creator_info/query/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header("Content-Type", "application/json; charset=UTF-8")
                .retrieve()
                .bodyToMono(CreatorInfoResponse.class)
                .block();
        log.debug("Creator Info Response: " + creatorInfoResponse);

        if (creatorInfoResponse == null) {
            throw new Exception("Tiktok Creator Info API response is empty");
        }

        List<String> imageUrls = imagesToUpload.stream()
                .map(ImageUrlEntry::getUrl)
                .toList();

        // Prepare Post Photo request for images
        PostPhotoRequest postPhotoRequest = new PostPhotoRequest(
                new PostPhotoRequestPostInfo(
                        "",
                        post.getCaption(),
                        false,
                        "PUBLIC_TO_EVERYONE",
                        false
                ),
                new PostPhotoRequestSourceInfo(
                        "PULL_FROM_URL",
                        0,
                        imageUrls
                ),
                "DIRECT_POST",
                "PHOTO"
        );

        PostPhotoResponse postPhotoResponse = tiktokWebClient.post()
                .uri("/post/publish/content/init/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(postPhotoRequest))
                .retrieve()
                .bodyToMono(PostPhotoResponse.class)
                .block();

        if (postPhotoResponse == null || postPhotoResponse.getData() == null|| postPhotoResponse.getError() == null) {
            throw new Exception("Tiktok Post photo API response is empty");
        }
        if (postPhotoResponse.getData().getPublishId() != null) {
            LogHelper.logWithSubject(log.atInfo().setMessage("Successfully upload to tiktok - publish_id: " + postPhotoResponse.getData().getPublishId()), post);
        } else {
            throw new Exception(String.format("Tiktok Post photo API call failed: %s - %s", postPhotoResponse.getError().getCode(), postPhotoResponse.getError().getCode()));
        }
    }
}
