package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.configuration.TiktokApiProperties;
import com.ko.footballupdater.models.ApiToken;
import com.ko.footballupdater.models.TokenPlatform;
import com.ko.footballupdater.repositories.ApiTokenRepository;
import com.ko.footballupdater.services.TiktokApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TiktokApiServiceTokenTest {

    private static final String REFRESH_RESPONSE = "{\"access_token\":\"new-access\",\"expires_in\":86400,"
            + "\"refresh_token\":\"new-refresh\",\"refresh_expires_in\":31536000,\"token_type\":\"Bearer\"}";
    private static final String ERROR_RESPONSE = "{\"error\":\"invalid_grant\",\"error_description\":\"Refresh token is invalid\"}";

    private TiktokApiService tiktokApiService;

    private ApiTokenRepository apiTokenRepository;

    private final List<ClientRequest> requests = new ArrayList<>();

    private String responseBody = REFRESH_RESPONSE;

    @BeforeEach
    public void setup() {
        requests.clear();
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            requests.add(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(responseBody)
                    .build());
        });
        tiktokApiService = new TiktokApiService(builder);

        TiktokApiProperties tiktokApiProperties = new TiktokApiProperties();
        tiktokApiProperties.setClientKey("client-key");
        tiktokApiProperties.setClientSecret("client-secret");

        apiTokenRepository = mock(ApiTokenRepository.class);
        when(apiTokenRepository.findById(TokenPlatform.TIKTOK)).thenReturn(Optional.empty());

        ReflectionTestUtils.setField(tiktokApiService, "tiktokApiProperties", tiktokApiProperties);
        ReflectionTestUtils.setField(tiktokApiService, "apiTokenRepository", apiTokenRepository);
    }

    private void givenPersistedToken(Instant accessExpiresAt, String refreshToken, Instant refreshExpiresAt) {
        ApiToken apiToken = new ApiToken(TokenPlatform.TIKTOK);
        apiToken.setAccessToken("old-access");
        apiToken.setExpiresAt(Date.from(accessExpiresAt));
        apiToken.setRefreshToken(refreshToken);
        apiToken.setRefreshExpiresAt(refreshExpiresAt != null ? Date.from(refreshExpiresAt) : null);
        when(apiTokenRepository.findById(TokenPlatform.TIKTOK)).thenReturn(Optional.of(apiToken));
        assertTrue(tiktokApiService.loadTokensFromDatabase());
    }

    @Test
    public void refreshTokenIfNeeded_accessTokenExpired_refreshesAndPersists() {
        givenPersistedToken(Instant.now().minus(Duration.ofHours(1)), "old-refresh", Instant.now().plus(Duration.ofDays(100)));

        tiktokApiService.refreshTokenIfNeeded();

        assertEquals(1, requests.size());
        assertEquals(HttpMethod.POST, requests.get(0).method());
        assertTrue(requests.get(0).url().toString().endsWith("/oauth/token/"));
        assertTrue(tiktokApiService.isTokenValid());
        assertEquals("new-access", ReflectionTestUtils.getField(tiktokApiService, "accessToken"));
        assertEquals("new-refresh", ReflectionTestUtils.getField(tiktokApiService, "refreshToken"));

        ArgumentCaptor<ApiToken> captor = ArgumentCaptor.forClass(ApiToken.class);
        verify(apiTokenRepository).save(captor.capture());
        assertEquals("new-access", captor.getValue().getAccessToken());
        assertEquals("new-refresh", captor.getValue().getRefreshToken());
        assertTrue(captor.getValue().getRefreshExpiresAt().after(Date.from(Instant.now().plus(Duration.ofDays(364)))));
    }

    @Test
    public void refreshTokenIfNeeded_accessTokenExpiringSoon_refreshes() {
        givenPersistedToken(Instant.now().plus(Duration.ofMinutes(30)), "old-refresh", null);

        tiktokApiService.refreshTokenIfNeeded();

        assertEquals(1, requests.size());
        assertEquals("new-access", ReflectionTestUtils.getField(tiktokApiService, "accessToken"));
    }

    @Test
    public void refreshTokenIfNeeded_accessTokenValid_doesNothing() {
        givenPersistedToken(Instant.now().plus(Duration.ofHours(20)), "old-refresh", Instant.now().plus(Duration.ofDays(100)));

        tiktokApiService.refreshTokenIfNeeded();

        assertTrue(requests.isEmpty());
        verify(apiTokenRepository, never()).save(any());
    }

    @Test
    public void refreshTokenIfNeeded_refreshTokenExpired_doesNothing() {
        givenPersistedToken(Instant.now().minus(Duration.ofHours(1)), "old-refresh", Instant.now().minus(Duration.ofDays(1)));

        tiktokApiService.refreshTokenIfNeeded();

        assertTrue(requests.isEmpty());
    }

    @Test
    public void refreshTokenIfNeeded_noRefreshToken_doesNothing() {
        tiktokApiService.refreshTokenIfNeeded();

        assertTrue(requests.isEmpty());
    }

    @Test
    public void refreshTokenIfNeeded_errorResponse_keepsExistingToken() {
        responseBody = ERROR_RESPONSE;
        givenPersistedToken(Instant.now().minus(Duration.ofHours(1)), "old-refresh", Instant.now().plus(Duration.ofDays(100)));

        tiktokApiService.refreshTokenIfNeeded();

        assertEquals(1, requests.size());
        assertEquals("old-access", ReflectionTestUtils.getField(tiktokApiService, "accessToken"));
        verify(apiTokenRepository, never()).save(any());
    }
}
