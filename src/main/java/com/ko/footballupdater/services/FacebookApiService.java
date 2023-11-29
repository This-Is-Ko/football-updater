package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.FacebookApiProperties;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.facebookApi.DebugTokenReponse;
import com.ko.footballupdater.models.facebookApi.FacebookAccessTokenResponse;
import com.ko.footballupdater.models.facebookApi.InstagramUserMedia;
import com.ko.footballupdater.models.form.FacebookApiDto;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.utils.FacebookApiHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FacebookApiService {

    @Autowired
    FacebookApiProperties facebookApiProperties;

    private final String FACEBOOK_API_BASE_URL = "https://www.facebook.com/v18.0/dialog/oauth";
    private final String FACEBOOK_GRAPH_API_BASE_URL = "https://graph.facebook.com/v18.0";

    private String state;
    private String accessToken;
    private Calendar expiresAt;

    private final WebClient facebookWebClient;
    private final WebClient facebookWebClientNoEncoding;

    public FacebookApiService(WebClient.Builder facebookWebClientBuilder, WebClient.Builder facebookWebClientBuilder1) {
        this.facebookWebClient = facebookWebClientBuilder.baseUrl(FACEBOOK_GRAPH_API_BASE_URL).filter(logRequest())
                .filter(logResponse()).build();
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        this.facebookWebClientNoEncoding = facebookWebClientBuilder1.uriBuilderFactory(defaultUriBuilderFactory)
                .baseUrl(FACEBOOK_GRAPH_API_BASE_URL).filter(logRequest())
                .filter(logResponse()).build();
    }

    public FacebookApiDto prepareFacebookApiDto() {
        FacebookApiDto facebookApiDto = new FacebookApiDto();
        if (isTokenValid()) {
            facebookApiDto.setCurrentlyLoggedIn(true);
        } else {
            facebookApiDto.setCurrentlyLoggedIn(false);
            facebookApiDto.setLoginUri(generateLoginString());
        }
        return facebookApiDto;
    }

    public Boolean isTokenValid() {
        if (accessToken == null || accessToken.isEmpty() || expiresAt == null) {
            return false;
        }
        Calendar currentTime = Calendar.getInstance();
        Calendar expiresAtMinusTenMin = ((Calendar) expiresAt.clone());
        // 5 minute buffer
        expiresAtMinusTenMin.add(Calendar.MINUTE, -5);

        return currentTime.before(expiresAtMinusTenMin);
    }

    public String generateLoginString() {
        UUID uuid = UUID.randomUUID();
        state = uuid.toString();
        return FACEBOOK_API_BASE_URL + String.format("?client_id=%s&response_type=%s&scope=%s&redirect_uri=%s&state=%s", facebookApiProperties.getClientId(), facebookApiProperties.getResponseType(), facebookApiProperties.getScope(), facebookApiProperties.getRedirectUri(), state);
    }

    public void handleLogin(String state, String code) throws Exception {
        verifyStateValue(state);
        // Exchange code for access token
        if (code != null && !code.isEmpty()) {
            FacebookAccessTokenResponse response = exchangeCodeForAccessToken(code);
            storeTokensInMemory(response);
        } else {
            throw new Exception("Login code param is empty");
        }
    }

    public void verifyStateValue(String state) throws Exception {
        if (!this.state.equals(state)) {
            throw new Exception("State values don't match");
        }
    }

    public void storeTokensInMemory(FacebookAccessTokenResponse response) throws Exception {
        if (response.getAccess_token() == null) {
            throw new Exception("Access token is null");
        }
        this.accessToken = response.getAccess_token();
        // Calculate token expiry time
        this.expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.SECOND, response.getExpires_in());
    }

    private FacebookAccessTokenResponse exchangeCodeForAccessToken(String code) {
        // Request URL structure:
        // /access_token?
        //   client_id={app-id}
        //   &redirect_uri={redirect-uri}
        //   &client_secret={app-secret}
        //   &code={code-parameter}
        String apiUrl = String.format("/oauth/access_token?client_id=%s&redirect_uri=%s&client_secret=%s&code=%s", facebookApiProperties.getClientId(), facebookApiProperties.getRedirectUri(), facebookApiProperties.getClientSecret(), code);
        return facebookWebClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(FacebookAccessTokenResponse.class)
                .block();
    }

    private DebugTokenReponse callDebugToken() throws Exception {
        if (!isTokenValid()) {
            throw new Exception("No access token available");
        }
        String apiUrl = String.format("/debug_token?input_token=%s&access_token=%s", accessToken, facebookApiProperties.getClientId() + "|" + facebookApiProperties.getClientSecret());
        return facebookWebClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(DebugTokenReponse.class)
                .block();
    }

    /**
     * From facebook spec
     * 1. Use the POST /{ig-user-id}/media endpoint to create individual item containers for each image and video that will appear in the carousel.
     * 2. Use the POST /{ig-user-id}/media endpoint again to create a single carousel container for the items.
     * 3. Use the POST /{ig-user-id}/media_publish endpoint to publish the carousel container.
     */
    public void postToInstagram(Post post, List<ImageUrlEntry> imagesToUpload, String caption) throws Exception {
        List<String> individualImageContainer = new ArrayList<>();
        log.atInfo().setMessage("Attempting to post to Instagram").addKeyValue("player", post.getPlayer().getName()).log();

        // Validate token has required permissions
//        DebugTokenReponse debugTokenReponse = callDebugToken();

        // Step 1
        imagesToUpload.forEach(imageUrlEntry -> {
            InstagramUserMedia response = null;
            try {
                response = callInstagramUserMediaApi(facebookApiProperties.getInstagram().getUserId(), imageUrlEntry.getUrl(), null, true, null, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (response.getId() != null && !response.getId().isEmpty()) {
                log.atInfo().setMessage("Successfully saved image to Instagram: " + imageUrlEntry.getImageIndex()).addKeyValue("player", post.getPlayer().getName()).log();
                individualImageContainer.add(response.getId());
            }
        });

        if (imagesToUpload.size() != individualImageContainer.size()) {
            throw new Exception("Images uploaded doesn't match expected");
        }

        // Step 2
        // Sample request "https://graph.facebook.com/v18.0/90010177253934/media?caption=Fruit%20candies&media_type=CAROUSEL&children=17899506308402767%2C18193870522147812%2C17853844403701904&access_token=EAAOc..."
        // Use caption if passed from form otherwise use prepared caption
        if (caption == null || caption.isEmpty()) {
            caption = post.getCaption();
        }
        InstagramUserMedia carouselCreateResponse = callInstagramUserMediaApi(facebookApiProperties.getInstagram().getUserId(), null, caption, false, "CAROUSEL", individualImageContainer);
        String carouselId = carouselCreateResponse.getId();
        log.atInfo().setMessage("Successfully created Instagram carousel container").addKeyValue("player", post.getPlayer().getName()).log();

        // Step 3
        InstagramUserMedia publishResponse = callInstagramUserMediaPublishApi(facebookApiProperties.getInstagram().getUserId(), carouselId);
        if (publishResponse == null || publishResponse.getId() == null || publishResponse.getId().isEmpty()) {
            throw new Exception("Attempt to publish carousel failed");
        }
        log.atInfo().setMessage("Successfully published carousel").addKeyValue("player", post.getPlayer().getName()).log();
    }

    public InstagramUserMedia callInstagramUserMediaApi(String instagramUserId, String imageUrl, String caption, Boolean isCarouselItem, String mediaType, List<String> children) throws Exception {
        // Encode uri before passing to custom webclient due to first # symbol not being encoded using default webclient settings

        // POST https://graph.facebook.com/{api-version}/{ig-user-id}/media
        //  ?image_url={image-url}
        //  &is_carousel_item={is-carousel-item}
        //  &access_token={access-token}

        if (!isTokenValid()) {
            throw new Exception("No access token available");
        }

        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(FACEBOOK_GRAPH_API_BASE_URL + "/" + instagramUserId + "/media");
        urlBuilder.queryParam("access_token", FacebookApiHelper.encodeTextToUtf8(accessToken));

        if (imageUrl != null) {
            urlBuilder.queryParam("image_url", FacebookApiHelper.encodeTextToUtf8(imageUrl));
        }

        if (mediaType != null) {
            urlBuilder.queryParam("media_type", mediaType);
        }

        if (children != null) {
            // Children param - e.g. children=17899506308402767%2C18193870522147812%2C17853844403701904
            String cihldrenString = String.join(",", children);
            urlBuilder.queryParam("children", FacebookApiHelper.encodeTextToUtf8(cihldrenString));
        }

        if (isCarouselItem != null) {
            urlBuilder.queryParam("is_carousel_item", isCarouselItem);
        }

        if (caption != null) {
            // Refer to https://stackoverflow.com/questions/54099777/inconsistent-line-breaks-when-posting-to-instagram
            // https://www.fileformat.info/info/unicode/char/2063/index.htm
            urlBuilder.queryParam("caption", FacebookApiHelper.encodeTextToUtf8(caption.replace("\n", "\u2063\n")));

        }
        String apiUrl = urlBuilder
                .build()
                .toUriString();

        return facebookWebClientNoEncoding.post()
                .uri(apiUrl)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Error in WebClient request. Response body: {}", errorBody);
                                    return Mono.error(new Exception("Unable to add Instagram user media"));
                                })
                )
                .bodyToMono(InstagramUserMedia.class)
                .block();
    }

    public InstagramUserMedia callInstagramUserMediaPublishApi(String instagramUserId, String creationId) throws Exception {
        // POST "https://graph.facebook.com/v18.0/90010177253934/media_publish?creation_id=18000748627392977&access_token=EAAOc..."

        if (accessToken == null || accessToken.isEmpty()) {
            throw new Exception("No access token available");
        }

        if (creationId == null || creationId.isEmpty()) {
            throw new Exception("No creationId, required for Instagram User Media Publish call");
        }
        StringBuilder apiUrlBuilder = new StringBuilder("/").append(instagramUserId).append("/media_publish?");
        apiUrlBuilder.append("access_token=").append(accessToken).append("&");
        apiUrlBuilder.append("creation_id=").append(creationId).append("&");

        String apiUrl = apiUrlBuilder.toString();

        return facebookWebClient.post()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(InstagramUserMedia.class)
                .block();
    }

    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            log.debug("Request: {} {}", clientRequest.method(), FacebookApiHelper.maskAccessToken(clientRequest.url().toString()));
            clientRequest.headers()
                    .forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
            return next.exchange(clientRequest);
        };
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Response: {}", clientResponse.headers().asHttpHeaders().get("property-header"));
            return Mono.just(clientResponse);
        });
    }

}
