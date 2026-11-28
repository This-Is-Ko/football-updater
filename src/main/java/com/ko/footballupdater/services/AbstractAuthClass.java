package com.ko.footballupdater.services;

import com.ko.footballupdater.models.ApiToken;
import com.ko.footballupdater.models.TokenPlatform;
import com.ko.footballupdater.models.facebookApi.AccessTokenResponse;
import com.ko.footballupdater.repositories.ApiTokenRepository;
import com.ko.footballupdater.utils.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public abstract class AbstractAuthClass {

    @Autowired
    ApiTokenRepository apiTokenRepository;

    String state;
    String accessToken;
    Calendar expiresAt;
    String refreshToken;
    Calendar refreshExpiresAt;

    /**
     * Platform used as the key when persisting tokens.
     */
    abstract TokenPlatform getPlatform();

    /**
     * Lifetime assumed when the token response doesn't include expires_in.
     */
    long defaultTokenLifetimeSeconds() {
        return 3600;
    }

    public void storeTokensInMemory(AccessTokenResponse response) throws Exception {
        if (response.getAccessToken() == null) {
            throw new Exception("Access token is null");
        }
        this.accessToken = response.getAccessToken();
        // Calculate token expiry time
        long expiresInSeconds = response.getExpiresIn() != null ? response.getExpiresIn() : defaultTokenLifetimeSeconds();
        this.expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.SECOND, (int) expiresInSeconds);

        if (response.getRefreshToken() != null && !response.getRefreshToken().isEmpty()) {
            this.refreshToken = response.getRefreshToken();
            if (response.getRefreshExpiresIn() != null) {
                this.refreshExpiresAt = Calendar.getInstance();
                refreshExpiresAt.add(Calendar.SECOND, response.getRefreshExpiresIn());
            } else {
                this.refreshExpiresAt = null;
            }
        }
        log.atInfo().setMessage(getPlatform() + " - Saved access token - Expires at: " + expiresAt.getTime()).log();
    }

    /**
     * Store tokens in memory and persist them so they survive restarts.
     * A failure to persist is logged but doesn't fail the login.
     */
    public void storeTokens(AccessTokenResponse response) throws Exception {
        storeTokensInMemory(response);
        persistTokens();
    }

    void persistTokens() {
        try {
            ApiToken apiToken = apiTokenRepository.findById(getPlatform()).orElseGet(() -> new ApiToken(getPlatform()));
            apiToken.setAccessToken(accessToken);
            apiToken.setExpiresAt(expiresAt != null ? expiresAt.getTime() : null);
            apiToken.setRefreshToken(refreshToken);
            apiToken.setRefreshExpiresAt(refreshExpiresAt != null ? refreshExpiresAt.getTime() : null);
            apiToken.setUpdatedAt(new Date());
            apiTokenRepository.save(apiToken);
            log.atInfo().setMessage(getPlatform() + " - Persisted access token").log();
        } catch (Exception ex) {
            log.atError().setMessage(getPlatform() + " - Unable to persist access token, it will only be kept in memory").setCause(ex).log();
        }
    }

    /**
     * Load previously persisted tokens into memory.
     * @return true if a token was found and loaded
     */
    public boolean loadTokensFromDatabase() {
        try {
            ApiToken apiToken = apiTokenRepository.findById(getPlatform()).orElse(null);
            if (apiToken == null || apiToken.getAccessToken() == null || apiToken.getAccessToken().isEmpty()) {
                log.atInfo().setMessage(getPlatform() + " - No persisted access token found").log();
                return false;
            }
            this.accessToken = apiToken.getAccessToken();
            this.expiresAt = toCalendar(apiToken.getExpiresAt());
            this.refreshToken = apiToken.getRefreshToken();
            this.refreshExpiresAt = toCalendar(apiToken.getRefreshExpiresAt());
            log.atInfo().setMessage(getPlatform() + " - Loaded persisted access token - Expires at: " + (expiresAt != null ? expiresAt.getTime() : "unknown")).log();
            return true;
        } catch (Exception ex) {
            log.atError().setMessage(getPlatform() + " - Unable to load persisted access token").setCause(ex).log();
            return false;
        }
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

    /**
     * @return true if there is an access token and it expires within the given duration (or has no known expiry)
     */
    boolean isTokenExpiringWithin(Duration duration) {
        if (accessToken == null || accessToken.isEmpty()) {
            return false;
        }
        if (expiresAt == null) {
            return true;
        }
        return expiresAt.getTimeInMillis() - System.currentTimeMillis() < duration.toMillis();
    }

    boolean isRefreshTokenValid() {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return false;
        }
        return refreshExpiresAt == null || Calendar.getInstance().before(refreshExpiresAt);
    }

    public Calendar getExpiresAt() {
        return expiresAt;
    }

    public void verifyStateValue(String state) throws Exception {
        if (this.state == null || !this.state.equals(state)) {
            throw new Exception("State values don't match");
        }
    }

    private static Calendar toCalendar(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            log.debug("Request: {} {}", clientRequest.method(), StringHelper.maskAccessToken(clientRequest.url().toString()));
            clientRequest.headers()
                    .forEach((name, values) -> values.forEach(value -> log.debug("{}={}", name,
                            HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name) ? "********" : value)));
            return next.exchange(clientRequest);
        };
    }

    ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Response: {}", clientResponse.headers().asHttpHeaders().get("property-header"));
            return Mono.just(clientResponse);
        });
    }
}
