package com.ko.footballupdater.services;

import com.ko.footballupdater.models.facebookApi.TiktokAccessTokenResponse;
import com.ko.footballupdater.utils.StringHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

import java.util.Calendar;

@Slf4j
public abstract class AbstractAuthClass {
    String state;
    String accessToken;
    Calendar expiresAt;

    public void storeTokensInMemory(TiktokAccessTokenResponse response) throws Exception {
        if (response.getAccess_token() == null) {
            throw new Exception("Access token is null");
        }
        this.accessToken = response.getAccess_token();
        // Calculate token expiry time
        this.expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.SECOND, response.getExpires_in());
        log.atInfo().setMessage("Saved access token - Expires at: " + expiresAt.toString()).log();
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

    public void verifyStateValue(String state) throws Exception {
        if (!this.state.equals(state)) {
            throw new Exception("State values don't match");
        }
    }

    ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            log.debug("Request: {} {}", clientRequest.method(), StringHelper.maskAccessToken(clientRequest.url().toString()));
            clientRequest.headers()
                    .forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
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
