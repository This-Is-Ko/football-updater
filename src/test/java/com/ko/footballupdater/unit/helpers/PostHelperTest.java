package com.ko.footballupdater.unit.helpers;

import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.utils.PostHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostHelperTest {

    @Test
    void testGeneratePostHashtags() {
        Player player = new Player("Test Player");
        Match match = new Match();
        match.setRelevantTeam("TeamA");
        PlayerMatchPerformanceStats playerMatchPerformanceStats = new PlayerMatchPerformanceStats(match);
        String additionalHashtags = "#AdditionalHashtags #AnotherOne";

        String result = PostHelper.generatePlayerHashtags(player);

        String expected = "\n\n#TestPlayer";
        assertEquals(expected, result);
    }

    @Test
    void testGeneratePostHashtagsNoTeam() {
        Player player = new Player("Test Player");
        Match match = new Match();
        PlayerMatchPerformanceStats playerMatchPerformanceStats = new PlayerMatchPerformanceStats(match);
        String additionalHashtags = "#AdditionalHashtags #AnotherOne";

        String result = PostHelper.generatePlayerHashtags(player);

        String expected = "\n\n#TestPlayer";
        assertEquals(expected, result);
    }

    @Test
    void testGeneratePostHashtagsAdditionalHashtags() {
        Player player = new Player("Test Player");
        Match match = new Match();
        match.setRelevantTeam("TeamA");
        PlayerMatchPerformanceStats playerMatchPerformanceStats = new PlayerMatchPerformanceStats(match);
        String additionalHashtags = null;

        String result = PostHelper.generatePlayerHashtags(player);

        String expected = "\n\n#TestPlayer";
        assertEquals(expected, result);
    }
}
