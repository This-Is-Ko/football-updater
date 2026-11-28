package com.ko.footballupdater.schedulers;

import com.ko.footballupdater.services.FacebookApiService;
import com.ko.footballupdater.services.TiktokApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically refreshes platform tokens before they expire. Each check only calls the platform
 * API when the token is within its refresh threshold.
 */
@Component
public class TokenRefreshScheduler {

    @Autowired
    private FacebookApiService facebookApiService;

    @Autowired
    private TiktokApiService tiktokApiService;

    @Scheduled(cron = "${scheduler.tokenRefresh.cron:0 0 * * * *}", zone = "${scheduler.zone:UTC}")
    public void refreshTokens() {
        facebookApiService.refreshTokenIfExpiringSoon();
        tiktokApiService.refreshTokenIfNeeded();
    }
}
