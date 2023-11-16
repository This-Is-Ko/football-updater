package com.ko.footballupdater.utils;

import com.ko.footballupdater.models.PlayerMatchPerformanceStats;

public class StatHelper {

    public static void populateStatPercentages(PlayerMatchPerformanceStats playerMatchPerformanceStats) {
        if (playerMatchPerformanceStats.getCrossesSuccessful() != null && playerMatchPerformanceStats.getCrosses() != null) {
            playerMatchPerformanceStats.setCrossesSuccessPercentage((int) ((float) playerMatchPerformanceStats.getCrossesSuccessful()/playerMatchPerformanceStats.getCrosses() * 100));
        }
        if (playerMatchPerformanceStats.getTacklesWon() != null && playerMatchPerformanceStats.getTackles() != null) {
            playerMatchPerformanceStats.setTacklesSuccessPercentage((int) ((float) playerMatchPerformanceStats.getTacklesWon()/playerMatchPerformanceStats.getTackles() * 100));
        }
        if (playerMatchPerformanceStats.getPassesCompleted() != null && playerMatchPerformanceStats.getPassesAttempted() != null) {
            playerMatchPerformanceStats.setPassesSuccessPercentage((int) ((float) playerMatchPerformanceStats.getPassesCompleted()/playerMatchPerformanceStats.getPassesAttempted() * 100));
        }
        if (playerMatchPerformanceStats.getCarriesSuccessful() != null && playerMatchPerformanceStats.getCarries() != null) {
            playerMatchPerformanceStats.setCarriesSuccessPercentage((int) ((float) playerMatchPerformanceStats.getCarriesSuccessful()/playerMatchPerformanceStats.getCarries() * 100));
        }
    }
}
