package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.FacebookApiProperties;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.facebookApi.DebugTokenReponse;
import com.ko.footballupdater.models.facebookApi.FacebookAccessTokenResponse;
import com.ko.footballupdater.models.facebookApi.InstagramUserMedia;
import com.ko.footballupdater.models.form.FacebookApiDto;
import com.ko.footballupdater.models.form.ImageUrlEntry;
import com.ko.footballupdater.utils.LogHelper;
import com.ko.footballupdater.utils.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FacebookApiService extends AbstractAuthClass {

    @Autowired
    FacebookApiProperties facebookApiProperties;

    private static final String FACEBOOK_API_BASE_URL = "https://www.facebook.com/v18.0/dialog/oauth";
    private static final String FACEBOOK_GRAPH_API_BASE_URL = "https://graph.facebook.com/v18.0";

    private final WebClient facebookWebClient;
    private final WebClient facebookWebClientNoEncoding;

    public FacebookApiService(WebClient.Builder facebookWebClientBuilder, WebClient.Builder facebookWebClientBuilder1) {
        this.facebookWebClient = facebookWebClientBuilder
                .baseUrl(FACEBOOK_GRAPH_API_BASE_URL)
                .filter(logRequest())
                .filter(logResponse()).build();
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        this.facebookWebClientNoEncoding = facebookWebClientBuilder1
                .uriBuilderFactory(defaultUriBuilderFactory)
                .baseUrl(FACEBOOK_GRAPH_API_BASE_URL)
                .filter(logRequest())
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

    public String generateLoginString() {
        UUID uuid = UUID.randomUUID();
        state = uuid.toString();
        return FACEBOOK_API_BASE_URL + String.format("?client_id=%s&response_type=%s&scope=%s&redirect_uri=%s&state=%s",
                facebookApiProperties.getClientId(),
                facebookApiProperties.getResponseType(),
                facebookApiProperties.getScope(),
                facebookApiProperties.getRedirectUri(),
                state);
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

    private FacebookAccessTokenResponse exchangeCodeForAccessToken(String code) {
        // Request URL structure:
        // /access_token?
        //   client_id={app-id}
        //   &redirect_uri={redirect-uri}
        //   &client_secret={app-secret}
        //   &code={code-parameter}
        String apiUrl = String.format("/oauth/access_token?client_id=%s&redirect_uri=%s&client_secret=%s&code=%s",
                facebookApiProperties.getClientId(), facebookApiProperties.getRedirectUri(), facebookApiProperties.getClientSecret(), code);
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
        String apiUrl = String.format("/debug_token?input_token=%s&access_token=%s",
                accessToken, facebookApiProperties.getClientId() + "|" + facebookApiProperties.getClientSecret());
        return facebookWebClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(DebugTokenReponse.class)
                .block();
    }

    /**
     * From Facebook spec:
     * For single image
     * 1. Use the POST /{ig-user-id}/media endpoint to create a container from an image or video hosted on your public server.
     * 2. Use the POST /{ig-user-id}/media_publish endpoint to publish the container.
     * Refer to <a href="https://developers.facebook.com/docs/instagram-api/guides/content-publishing#single-media-posts">...</a>
     * ----
     * For multiple images
     * 1. Use the POST /{ig-user-id}/media endpoint to create individual item containers for each image and video that will appear in the carousel.
     * 2. Use the POST /{ig-user-id}/media endpoint again to create a single carousel container for the items.
     * 3. Use the POST /{ig-user-id}/media_publish endpoint to publish the carousel container.
     * Refer to <a href="https://developers.facebook.com/docs/instagram-api/guides/content-publishing#carousel-posts">...</a>
     */
    public void postToInstagram(Post post, List<ImageUrlEntry> imagesToUpload) throws Exception {
        LogHelper.logWithSubject(log.atInfo().setMessage("Attempting to post to Instagram"), post);

        // Validate token has required permissions
//        DebugTokenReponse debugTokenReponse = callDebugToken();

        // Single image posting
        if (imagesToUpload.size() == 1) {
            // Step 1 - create container
            InstagramUserMedia response;
            try {
                response = callInstagramUserMediaApi(facebookApiProperties.getInstagram().getUserId(), imagesToUpload.get(0).getUrl(), post.getCaption(), false, null, null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (response.getId() != null && !response.getId().isEmpty()) {
                LogHelper.logWithSubject(log.atInfo().setMessage("Successfully saved single image to Instagram"), post);
            } else {
                throw new RuntimeException("No container ID in Instagram User Media Api response");
            }

            // Step 2 - publish container
            InstagramUserMedia publishResponse = callInstagramUserMediaPublishApi(facebookApiProperties.getInstagram().getUserId(), response.getId());
            if (publishResponse == null || publishResponse.getId() == null || publishResponse.getId().isEmpty()) {
                throw new Exception("Attempt to publish single image post failed");
            }
            LogHelper.logWithSubject(log.atInfo().setMessage("Successfully published single image post"), post);

        } else {
            // Multiple images, use carousel (does not allow single images)
            List<String> individualImageContainer = new ArrayList<>();

            // Step 1 - create item container
            imagesToUpload.forEach(imageUrlEntry -> {
                InstagramUserMedia response;
                try {
                    response = callInstagramUserMediaApi(facebookApiProperties.getInstagram().getUserId(), imageUrlEntry.getUrl(), null, true, null, null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if (response.getId() != null && !response.getId().isEmpty()) {
                    LogHelper.logWithSubject(log.atInfo().setMessage("Successfully saved image to Instagram: " + imageUrlEntry.getImageIndex()), post);

                    individualImageContainer.add(response.getId());
                }
            });

            if (imagesToUpload.size() != individualImageContainer.size()) {
                throw new Exception("Images uploaded doesn't match expected");
            }

            // Step 2 - create carousel container
            // Sample request "https://graph.facebook.com/v18.0/90010177253934/media?caption=Fruit%20candies&media_type=CAROUSEL&children=17899506308402767%2C18193870522147812%2C17853844403701904&access_token=EAAOc..."
            InstagramUserMedia carouselCreateResponse = callInstagramUserMediaApi(facebookApiProperties.getInstagram().getUserId(), null, post.getCaption(), false, "CAROUSEL", individualImageContainer);
            String carouselId = carouselCreateResponse.getId();
            LogHelper.logWithSubject(log.atInfo().setMessage("Successfully created Instagram carousel container with id=" + carouselId), post);

            // Step 3 - publish carousel container
            InstagramUserMedia publishResponse = callInstagramUserMediaPublishApi(facebookApiProperties.getInstagram().getUserId(), carouselId);
            if (publishResponse == null || publishResponse.getId() == null || publishResponse.getId().isEmpty()) {
                throw new Exception("Attempt to publish carousel failed");
            }
            LogHelper.logWithSubject(log.atInfo().setMessage("Successfully published carousel"), post);
        }
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
        urlBuilder.queryParam("access_token", StringHelper.encodeTextToUtf8(accessToken));

        if (imageUrl != null) {
            urlBuilder.queryParam("image_url", StringHelper.encodeTextToUtf8(imageUrl));
        }

        if (mediaType != null) {
            urlBuilder.queryParam("media_type", mediaType);
        }

        if (children != null) {
            // Children param - e.g. children=17899506308402767%2C18193870522147812%2C17853844403701904
            String cihldrenString = String.join(",", children);
            urlBuilder.queryParam("children", StringHelper.encodeTextToUtf8(cihldrenString));
        }

        if (isCarouselItem != null) {
            urlBuilder.queryParam("is_carousel_item", isCarouselItem);
        }

        if (caption != null) {
            // Refer to https://stackoverflow.com/questions/54099777/inconsistent-line-breaks-when-posting-to-instagram
            // https://www.fileformat.info/info/unicode/char/2063/index.htm
            // Maximum 2200 characters, 30 hashtags, and 20 @ tags
            urlBuilder.queryParam("caption", StringHelper.encodeTextToUtf8(caption.replace("\n", "\u2063\n")));
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
}
