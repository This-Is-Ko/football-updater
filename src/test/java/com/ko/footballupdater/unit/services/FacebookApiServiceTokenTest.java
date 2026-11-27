package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.configuration.FacebookApiProperties;
import com.ko.footballupdater.models.ApiToken;
import com.ko.footballupdater.models.TokenPlatform;
import com.ko.footballupdater.repositories.ApiTokenRepository;
import com.ko.footballupdater.services.FacebookApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FacebookApiServiceTokenTest {

    private static final String SHORT_LIVED_TOKEN_RESPONSE = "{\"access_token\":\"short-token\",\"token_type\":\"bearer\",\"expires_in\":3600}";
    private static final String LONG_LIVED_TOKEN_RESPONSE = "{\"access_token\":\"long-token\",\"token_type\":\"bearer\",\"expires_in\":5184000}";

    private FacebookApiService facebookApiService;

    private ApiTokenRepository apiTokenRepository;

    private final List<ClientRequest> requests = new ArrayList<>();

    @BeforeEach
    public void setup() {
        requests.clear();
        facebookApiService = new FacebookApiService(stubWebClientBuilder(), stubWebClientBuilder());

        FacebookApiProperties facebookApiProperties = new FacebookApiProperties();
        facebookApiProperties.setClientId("client-id");
        facebookApiProperties.setClientSecret("client-secret");
        facebookApiProperties.setRedirectUri("http://localhost/facebook/auth");
        facebookApiProperties.setTokenRefreshThresholdDays(10);

        apiTokenRepository = mock(ApiTokenRepository.class);
        when(apiTokenRepository.findById(TokenPlatform.INSTAGRAM)).thenReturn(Optional.empty());

        ReflectionTestUtils.setField(facebookApiService, "facebookApiProperties", facebookApiProperties);
        ReflectionTestUtils.setField(facebookApiService, "apiTokenRepository", apiTokenRepository);
    }

    private WebClient.Builder stubWebClientBuilder() {
        return WebClient.builder().exchangeFunction(request -> {
            requests.add(request);
            String body = request.url().toString().contains("fb_exchange_token") ? LONG_LIVED_TOKEN_RESPONSE : SHORT_LIVED_TOKEN_RESPONSE;
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .build());
        });
    }

    private void givenPersistedToken(String token, Instant expiresAt) {
        ApiToken apiToken = new ApiToken(TokenPlatform.INSTAGRAM);
        apiToken.setAccessToken(token);
        apiToken.setExpiresAt(Date.from(expiresAt));
        when(apiTokenRepository.findById(TokenPlatform.INSTAGRAM)).thenReturn(Optional.of(apiToken));
        assertTrue(facebookApiService.loadTokensFromDatabase());
    }

    @Test
    public void handleLogin_validCode_exchangesForLongLivedTokenAndPersists() throws Exception {
        facebookApiService.generateLoginString();
        String state = (String) ReflectionTestUtils.getField(facebookApiService, "state");

        facebookApiService.handleLogin(state, "auth-code");

        assertEquals(2, requests.size());
        assertTrue(requests.get(0).url().toString().contains("code=auth-code"));
        String exchangeUrl = requests.get(1).url().toString();
        assertTrue(exchangeUrl.contains("/oauth/access_token"));
        assertTrue(exchangeUrl.contains("grant_type=fb_exchange_token"));
        assertTrue(exchangeUrl.contains("fb_exchange_token=short-token"));

        assertEquals("long-token", ReflectionTestUtils.getField(facebookApiService, "accessToken"));
        assertTrue(facebookApiService.isTokenValid());
        // ~60 days from the expires_in in the long-lived response
        long daysLeft = Duration.between(Instant.now(), facebookApiService.getExpiresAt().toInstant()).toDays();
        assertTrue(daysLeft >= 59 && daysLeft <= 60);

        ArgumentCaptor<ApiToken> captor = ArgumentCaptor.forClass(ApiToken.class);
        verify(apiTokenRepository).save(captor.capture());
        assertEquals(TokenPlatform.INSTAGRAM, captor.getValue().getPlatform());
        assertEquals("long-token", captor.getValue().getAccessToken());
        assertEquals(facebookApiService.getExpiresAt().getTime(), captor.getValue().getExpiresAt());
    }

    @Test
    public void handleLogin_persistFails_tokenStillUsable() throws Exception {
        when(apiTokenRepository.save(any())).thenThrow(new RuntimeException("Table doesn't exist"));
        facebookApiService.generateLoginString();
        String state = (String) ReflectionTestUtils.getField(facebookApiService, "state");

        facebookApiService.handleLogin(state, "auth-code");

        assertTrue(facebookApiService.isTokenValid());
    }

    @Test
    public void loadTokensFromDatabase_noToken_returnsFalse() {
        assertFalse(facebookApiService.loadTokensFromDatabase());
        assertFalse(facebookApiService.isTokenValid());
    }

    @Test
    public void loadTokensFromDatabase_repositoryThrows_returnsFalse() {
        when(apiTokenRepository.findById(TokenPlatform.INSTAGRAM)).thenThrow(new RuntimeException("DB down"));
        assertFalse(facebookApiService.loadTokensFromDatabase());
    }

    @Test
    public void isTokenValid_persistedTokenExpired_returnsFalse() {
        givenPersistedToken("old-token", Instant.now().minus(Duration.ofDays(1)));
        assertFalse(facebookApiService.isTokenValid());
    }

    @Test
    public void isTokenValid_persistedTokenNotExpired_returnsTrue() {
        givenPersistedToken("token", Instant.now().plus(Duration.ofDays(30)));
        assertTrue(facebookApiService.isTokenValid());
    }

    @Test
    public void refreshTokenIfExpiringSoon_withinThreshold_refreshesAndPersists() {
        givenPersistedToken("expiring-token", Instant.now().plus(Duration.ofDays(5)));

        facebookApiService.refreshTokenIfExpiringSoon();

        assertEquals(1, requests.size());
        assertTrue(requests.get(0).url().toString().contains("fb_exchange_token=expiring-token"));
        assertEquals("long-token", ReflectionTestUtils.getField(facebookApiService, "accessToken"));
        verify(apiTokenRepository).save(any(ApiToken.class));
    }

    @Test
    public void refreshTokenIfExpiringSoon_outsideThreshold_doesNothing() {
        givenPersistedToken("fresh-token", Instant.now().plus(Duration.ofDays(30)));

        facebookApiService.refreshTokenIfExpiringSoon();

        assertTrue(requests.isEmpty());
        verify(apiTokenRepository, never()).save(any());
    }

    @Test
    public void refreshTokenIfExpiringSoon_tokenExpired_doesNothing() {
        givenPersistedToken("expired-token", Instant.now().minus(Duration.ofHours(1)));

        facebookApiService.refreshTokenIfExpiringSoon();

        assertTrue(requests.isEmpty());
        verify(apiTokenRepository, never()).save(any());
    }

    @Test
    public void refreshTokenIfExpiringSoon_noToken_doesNothing() {
        facebookApiService.refreshTokenIfExpiringSoon();

        assertTrue(requests.isEmpty());
    }
}
