package com.ko.footballupdater.unit.llm;

import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.PostType;

import java.util.Calendar;
import java.util.Date;

public final class LlmTestData {

    private LlmTestData() {
    }

    public static Date matchDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.NOVEMBER, 22, 15, 0, 0);
        return calendar.getTime();
    }

    /**
     * Northfield FC 3-1 Southport United, player with 2 goals, 1 assist, 90 minutes, 45 passes at 88%.
     */
    public static Post post(int id, String playerName) {
        Match match = new Match("https://example.com/match", matchDate(), "Northfield FC", "Southport United", "Northfield FC", 3, 1);
        PlayerMatchPerformanceStats stats = PlayerMatchPerformanceStats.builder()
                .match(match)
                .minutesPlayed(90)
                .goals(2)
                .assists(1)
                .shots(4)
                .passesAttempted(45)
                .xg(0.87f)
                .build();
        stats.setPassesSuccessPercentage(88);
        Post post = new Post(PostType.ALL_STAT_POST, new Player(playerName), stats);
        post.setId(id);
        post.setCaption("template caption");
        return post;
    }
}
