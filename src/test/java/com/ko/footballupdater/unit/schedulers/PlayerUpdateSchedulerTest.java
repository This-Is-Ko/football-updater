package com.ko.footballupdater.unit.schedulers;

import com.ko.footballupdater.responses.UpdatePlayersResponse;
import com.ko.footballupdater.schedulers.PlayerUpdateScheduler;
import com.ko.footballupdater.schedulers.TokenRefreshScheduler;
import com.ko.footballupdater.services.FacebookApiService;
import com.ko.footballupdater.services.PlayerService;
import com.ko.footballupdater.services.TiktokApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerUpdateSchedulerTest {

    @InjectMocks
    private PlayerUpdateScheduler playerUpdateScheduler;

    @InjectMocks
    private TokenRefreshScheduler tokenRefreshScheduler;

    @Mock
    private PlayerService playerService;

    @Mock
    private FacebookApiService facebookApiService;

    @Mock
    private TiktokApiService tiktokApiService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void updateAllPlayers_callsPlayerService() throws Exception {
        when(playerService.updateDataForAllPlayers()).thenReturn(new UpdatePlayersResponse());

        playerUpdateScheduler.updateAllPlayers();

        verify(playerService, times(1)).updateDataForAllPlayers();
    }

    @Test
    public void updateAllPlayers_serviceThrows_exceptionNotPropagated() throws Exception {
        when(playerService.updateDataForAllPlayers()).thenThrow(new RuntimeException("Data source down"));

        assertDoesNotThrow(() -> playerUpdateScheduler.updateAllPlayers());
    }

    @Test
    public void updateAllPlayers_cronIsConfigurableAndDisabledByDefault() throws Exception {
        Method method = PlayerUpdateScheduler.class.getMethod("updateAllPlayers");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("${scheduler.playerUpdate.cron:-}", scheduled.cron());
    }

    @Test
    public void refreshTokens_refreshesBothPlatforms() {
        tokenRefreshScheduler.refreshTokens();

        verify(facebookApiService, times(1)).refreshTokenIfExpiringSoon();
        verify(tiktokApiService, times(1)).refreshTokenIfNeeded();
    }
}
