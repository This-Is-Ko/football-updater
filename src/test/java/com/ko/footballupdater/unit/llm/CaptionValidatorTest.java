package com.ko.footballupdater.unit.llm;

import com.ko.footballupdater.llm.CaptionValidator;
import com.ko.footballupdater.llm.GroundingValidator;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.utils.StatFieldHelper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CaptionValidatorTest {

    private final Post post = LlmTestData.post(1, "Jamie Rivers");
    private final Set<String> allowedNumbers = StatFieldHelper.allowedNumbers(post);
    private final Set<String> knownNames = Set.of("Jamie Rivers", "Casey Moor", "Northfield FC", "Southport United", "Eastbrook Town");

    private Map<String, String> placeholders() {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("player", "Jamie Rivers");
        placeholders.put("team", "Northfield FC");
        placeholders.put("opponent", "Southport United");
        return placeholders;
    }

    private List<String> validate(String body) {
        return CaptionValidator.validate(body, placeholders(), allowedNumbers, knownNames, "\n\n#jamierivers #nfc");
    }

    @Test
    public void allowedNumbers_includesStatsScoresDateAndFloatRoundings() {
        assertTrue(allowedNumbers.containsAll(Set.of("90", "2", "1", "4", "45", "88", "3", "0.87", "0.9", "22", "11", "2026")));
        assertFalse(allowedNumbers.contains("5"));
    }

    @Test
    public void validate_groundedCaption_noErrors() {
        List<String> errors = validate("{player} with 2 goals and 1 assist for {team} 🔥 What a day vs {opponent}.\nSwipe for all 90 minutes ➡️");
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    public void validate_numbersAsWordsThatMatchStats_noErrors() {
        assertTrue(validate("A brace for {player} in a big win over {opponent}").isEmpty());
    }

    @Test
    public void validate_inventedNumber_error() {
        List<String> errors = validate("{player} scored 3 goals in 70 minutes");
        // 3 is the home score so it's allowed, 70 isn't in the data
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("70"));
    }

    @Test
    public void validate_inventedNumberWord_error() {
        List<String> errors = validate("A hat-trick for {player}! Five shots on target too");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Five")));
    }

    @Test
    public void validate_decimalNotInStats_error() {
        assertFalse(validate("{player} finished with 1.5 xG").isEmpty());
        assertTrue(validate("{player} finished with 0.87 xG").isEmpty());
    }

    @Test
    public void validate_literalOtherPlayerName_error() {
        List<String> errors = validate("{player} and Casey Moor linked up again");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Casey")));
    }

    @Test
    public void validate_otherTeamFromDatabaseAtSentenceStart_error() {
        List<String> errors = validate("Eastbrook Town will be watching. {player} was everywhere");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Eastbrook Town")));
    }

    @Test
    public void validate_unknownCapitalisedNameMidSentence_error() {
        List<String> errors = validate("{player} outshone Rodriguez today");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Rodriguez")));
    }

    @Test
    public void validate_correctLiteralNames_noErrors() {
        assertTrue(validate("Big day for Jamie Rivers and Northfield FC").isEmpty());
    }

    @Test
    public void validate_hashtagsMentionsLinksAndUnknownPlaceholders_errors() {
        assertFalse(validate("{player} #motm").isEmpty());
        assertFalse(validate("{player} with @someone").isEmpty());
        assertFalse(validate("{player} https://example.com").isEmpty());
        assertFalse(validate("{player} vs {referee}").isEmpty());
        assertFalse(validate("").isEmpty());
        assertFalse(validate(null).isEmpty());
    }

    @Test
    public void validate_tooLong_error() {
        assertFalse(validate("{player} " + "great ".repeat(300)).isEmpty());
    }

    @Test
    public void render_replacesPlaceholdersWithDatabaseNames() {
        assertEquals("Jamie Rivers for Northfield FC", CaptionValidator.render("{player} for {team}", placeholders()));
    }

    @Test
    public void findUngroundedNumbers_ignoresHashtags() {
        assertTrue(GroundingValidator.findUngroundedNumbers("Great game #u21 #2026season", Set.of()).isEmpty());
    }
}
