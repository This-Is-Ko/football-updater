package com.ko.footballupdater.schedulers;

import com.ko.footballupdater.responses.UpdatePlayersResponse;
import com.ko.footballupdater.services.PlayerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the player data update on a schedule, as an alternative to an external cron job calling GET /api/player/data/update.
 * Disabled by default, set scheduler.playerUpdate.cron (SCHEDULER_PLAYER_UPDATE_CRON) to enable.
 */
@Slf4j
@Component
public class PlayerUpdateScheduler {

    @Autowired
    private PlayerService playerService;

    @Scheduled(cron = "${scheduler.playerUpdate.cron:-}", zone = "${scheduler.zone:UTC}")
    public void updateAllPlayers() {
        log.atInfo().setMessage("Scheduled player data update started").log();
        try {
            UpdatePlayersResponse response = playerService.updateDataForAllPlayers();
            log.atInfo().setMessage("Scheduled player data update completed - players updated: " + response.getNumPlayersUpdated()).log();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.atError().setMessage("Scheduled player data update interrupted").setCause(ex).log();
        } catch (Exception ex) {
            log.atError().setMessage("Scheduled player data update failed").setCause(ex).log();
        }
    }
}
