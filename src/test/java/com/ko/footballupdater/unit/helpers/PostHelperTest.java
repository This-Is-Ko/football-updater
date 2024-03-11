package com.ko.footballupdater.unit.helpers;

import com.ko.footballupdater.models.Hashtag;
import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Team;
import com.ko.footballupdater.repositories.TeamRepository;
import com.ko.footballupdater.services.PlayerService;
import com.ko.footballupdater.utils.PostHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostHelperTest {

    @InjectMocks
    private PostHelper postHelper;
    @Mock
    private TeamRepository teamRepository;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGeneratePostHashtags() {
        Player player = new Player("Test Player");

        String result = postHelper.generatePlayerHashtags(player);

        String expected = "\n\n#testplayer";
        assertEquals(expected, result);
    }

    @Test
    void testGeneratePostHashtagsNoTeam() {
        Player player = new Player("Test Player");

        String result = postHelper.generatePlayerHashtags(player);

        String expected = "\n\n#testplayer";
        assertEquals(expected, result);
    }

    @Test
    void testGeneratePostHashtagsAdditionalHashtags() {
        Player player = new Player("Test Player");

        String result = postHelper.generatePlayerHashtags(player);

        String expected = "\n\n#testplayer";
        assertEquals(expected, result);
    }

    @Test
    public void generateTeamHashtags_foundEntryWithTeamName_returnHashtags() {
        String teamName = "ValidTeam";
        Team mockTeam = mock(Team.class);
        when(mockTeam.getAdditionalHashtags()).thenReturn(Set.of(new Hashtag("#hashtag1"), new Hashtag("#hashtag2")));

        when(teamRepository.findByName(teamName)).thenReturn(List.of(mockTeam));

        String hashtags = postHelper.generateTeamHashtags(teamName);

        // Check contains due to random set order
        assertTrue(hashtags.contains(" #hashtag1"));
        assertTrue(hashtags.contains(" #hashtag2"));
    }

    @Test
    public void generateTeamHashtags_foundEntryWithAltName_returnHashtags() {
        String teamName = "ValidTeamWithAltName";
        Team mockTeam = mock(Team.class);
        when(mockTeam.getAdditionalHashtags()).thenReturn(Set.of(new Hashtag("#hashtag1"), new Hashtag("#hashtag2")));

        when(teamRepository.findByName(teamName)).thenReturn(Collections.emptyList());
        when(teamRepository.findByAlternativeTeamName(teamName.toLowerCase())).thenReturn(List.of(mockTeam));

        String hashtags = postHelper.generateTeamHashtags(teamName);

        // Check contains due to random set order
        assertTrue(hashtags.contains(" #hashtag1"));
        assertTrue(hashtags.contains(" #hashtag2"));
    }

    @Test
    public void generateTeamHashtags_cannotFindEntryWithTeamNameOrAltName_returnGeneratedHashtags() {
        String teamName = "Invalid Team";
        when(teamRepository.findByName(teamName)).thenReturn(Collections.emptyList());
        when(teamRepository.findByAlternativeTeamName(teamName.toLowerCase())).thenReturn(Collections.emptyList());

        String hashtags = postHelper.generateTeamHashtags(teamName);

        assertEquals(" #invalidteam", hashtags);
    }

    @Test
    public void generateTeamHashtags_nullTeamName_returnEmptyString() {
        String hashtags = postHelper.generateTeamHashtags(null);

        assertEquals(" ", hashtags);
    }

    @Test
    public void generateTeamHashtags_emptyTeamName_returnEmptyString() {
        String hashtags = postHelper.generateTeamHashtags("");

        assertEquals(" ", hashtags);
    }
}
