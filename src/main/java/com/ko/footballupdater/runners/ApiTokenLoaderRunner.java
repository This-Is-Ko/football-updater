package com.ko.footballupdater.runners;

import com.ko.footballupdater.services.FacebookApiService;
import com.ko.footballupdater.services.TiktokApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Loads persisted platform tokens on startup and refreshes any that are close to expiring.
 */
@Slf4j
@Component
public class ApiTokenLoaderRunner implements ApplicationRunner {

    @Autowired
    private FacebookApiService facebookApiService;

    @Autowired
    private TiktokApiService tiktokApiService;

    @Override
    public void run(ApplicationArguments args) {
        if (facebookApiService.loadTokensFromDatabase()) {
            facebookApiService.refreshTokenIfExpiringSoon();
        }
        if (tiktokApiService.loadTokensFromDatabase()) {
            tiktokApiService.refreshTokenIfNeeded();
        }
    }
}
