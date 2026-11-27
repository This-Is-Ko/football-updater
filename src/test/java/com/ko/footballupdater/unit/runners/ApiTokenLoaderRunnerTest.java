package com.ko.footballupdater.unit.runners;

import com.ko.footballupdater.runners.ApiTokenLoaderRunner;
import com.ko.footballupdater.services.FacebookApiService;
import com.ko.footballupdater.services.TiktokApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiTokenLoaderRunnerTest {

    @InjectMocks
    private ApiTokenLoaderRunner apiTokenLoaderRunner;

    @Mock
    private FacebookApiService facebookApiService;

    @Mock
    private TiktokApiService tiktokApiService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void run_tokensPersisted_loadsAndRefreshes() {
        when(facebookApiService.loadTokensFromDatabase()).thenReturn(true);
        when(tiktokApiService.loadTokensFromDatabase()).thenReturn(true);

        apiTokenLoaderRunner.run(null);

        verify(facebookApiService).refreshTokenIfExpiringSoon();
        verify(tiktokApiService).refreshTokenIfNeeded();
    }

    @Test
    public void run_noTokensPersisted_skipsRefresh() {
        when(facebookApiService.loadTokensFromDatabase()).thenReturn(false);
        when(tiktokApiService.loadTokensFromDatabase()).thenReturn(false);

        apiTokenLoaderRunner.run(null);

        verify(facebookApiService, never()).refreshTokenIfExpiringSoon();
        verify(tiktokApiService, never()).refreshTokenIfNeeded();
    }
}
