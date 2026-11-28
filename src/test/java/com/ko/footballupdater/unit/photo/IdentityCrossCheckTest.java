package com.ko.footballupdater.unit.photo;

import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.LlmException;
import com.ko.footballupdater.models.AlternativePlayerName;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.photo.PhotoCandidate;
import com.ko.footballupdater.photo.face.IdentityCrossCheck;
import com.ko.footballupdater.photo.face.IdentityCrossCheck.CaptionMention;
import com.ko.footballupdater.photo.face.IdentityCrossCheck.IdentityReadResponse;
import com.ko.footballupdater.photo.face.IdentityCrossCheck.Verdict;
import com.ko.footballupdater.unit.llm.StubLlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IdentityCrossCheckTest {

    private IdentityCrossCheck crossCheck;
    private StubLlmClient llm;
    private Player player;
    private final BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
    private final PhotoCandidate candidate = new PhotoCandidate("https://p/x.jpg", "FAKE", null, "Jamie Rivers celebrates", null);

    @BeforeEach
    public void setup() {
        crossCheck = new IdentityCrossCheck();
        llm = new StubLlmClient();
        LlmProperties llmProperties = new LlmProperties();
        ReflectionTestUtils.setField(crossCheck, "llmClient", llm);
        ReflectionTestUtils.setField(crossCheck, "llmProperties", llmProperties);
        player = new Player("Jamie Rivers-Ölund");
        player.setShirtNumber(10);
        player.setAlternativeNames(Set.of(new AlternativePlayerName("J. Rivers")));
    }

    @Test
    public void matchingNumberAndName_consistent() {
        llm.thenReturn(new IdentityReadResponse("10", "OLUND", CaptionMention.EXPECTED_PLAYER, "read"));

        IdentityCrossCheck.Result result = crossCheck.check(player, candidate, image);

        assertEquals(Verdict.CONSISTENT, result.verdict());
        assertEquals(1, llm.calls.size());
        assertEquals(1, llm.calls.get(0).images().size());
        assertTrue(llm.calls.get(0).userPrompt().contains("Jamie Rivers celebrates"));
        assertTrue(llm.calls.get(0).systemPrompt().contains("Do not identify anyone from their face"));
    }

    @Test
    public void wrongNumber_contradicts() {
        llm.thenReturn(new IdentityReadResponse("9", null, CaptionMention.NOT_STATED, null));

        IdentityCrossCheck.Result result = crossCheck.check(player, candidate, image);

        assertEquals(Verdict.CONTRADICTS, result.verdict());
        assertTrue(result.reasons().get(0).contains("wears 10"));
    }

    @Test
    public void wrongShirtName_contradicts() {
        llm.thenReturn(new IdentityReadResponse(null, "KERR", null, null));

        assertEquals(Verdict.CONTRADICTS, crossCheck.check(player, candidate, image).verdict());
    }

    @Test
    public void captionNamesSomeoneElse_contradicts() {
        llm.thenReturn(new IdentityReadResponse(null, null, CaptionMention.DIFFERENT_PLAYER, null));

        assertEquals(Verdict.CONTRADICTS, crossCheck.check(player, candidate, image).verdict());
    }

    @Test
    public void unknownShirtNumber_numberIgnored() {
        player.setShirtNumber(null);
        llm.thenReturn(new IdentityReadResponse("9", null, CaptionMention.NOT_STATED, null));

        assertEquals(Verdict.UNKNOWN, crossCheck.check(player, candidate, image).verdict());
    }

    @Test
    public void nothingReadable_unknown() {
        llm.thenReturn(new IdentityReadResponse("1?", "", CaptionMention.NOT_STATED, null));

        assertEquals(Verdict.UNKNOWN, crossCheck.check(player, candidate, image).verdict());
    }

    @Test
    public void llmFails_unknown() {
        llm.thenThrow(new LlmException("boom"));

        assertEquals(Verdict.UNKNOWN, crossCheck.check(player, candidate, image).verdict());
    }

    @Test
    public void llmDisabled_notCalled() {
        llm.enabled(false);

        assertEquals(Verdict.UNKNOWN, crossCheck.check(player, candidate, image).verdict());
        assertTrue(llm.calls.isEmpty());
    }
}
