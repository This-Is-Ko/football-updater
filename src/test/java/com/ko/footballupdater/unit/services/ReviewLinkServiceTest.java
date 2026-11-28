package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.configuration.ReviewLinkProperties;
import com.ko.footballupdater.models.DraftStatus;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.repositories.PostRepository;
import com.ko.footballupdater.services.ReviewLinkService;
import com.ko.footballupdater.unit.llm.LlmTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReviewLinkServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-28T00:00:00Z");

    @InjectMocks
    private ReviewLinkService reviewLinkService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ReviewLinkProperties reviewLinkProperties;

    private Post post;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(reviewLinkProperties.isEnabled()).thenReturn(true);
        when(reviewLinkProperties.getBaseUrl()).thenReturn("https://updater.example.com/");
        when(reviewLinkProperties.getSecret()).thenReturn("test-secret-that-is-at-least-32-characters");
        when(reviewLinkProperties.getExpiryHours()).thenReturn(48L);
        reviewLinkService.setClock(Clock.fixed(NOW, ZoneOffset.UTC));
        post = LlmTestData.post(7, "Jamie Rivers");
        post.setDraftStatus(DraftStatus.PENDING_REVIEW);
        when(postRepository.findById(7)).thenReturn(Optional.of(post));
    }

    private String token(String link) {
        return link.substring(link.lastIndexOf('/') + 1);
    }

    @Test
    public void issuedLink_resolvesToPost() {
        String link = reviewLinkService.issueLink(post);
        assertTrue(link.startsWith("https://updater.example.com/review/7."));
        assertEquals(Optional.of(post), reviewLinkService.resolve(token(link)));
    }

    @Test
    public void tamperedToken_rejected() {
        String token = token(reviewLinkService.issueLink(post));
        // Point a valid signature at a different post id
        String tampered = "8" + token.substring(1);
        when(postRepository.findById(8)).thenReturn(Optional.of(post));
        assertFalse(reviewLinkService.resolve(tampered).isPresent());
        assertFalse(reviewLinkService.resolve(token + "x").isPresent());
        assertFalse(reviewLinkService.resolve("garbage").isPresent());
    }

    @Test
    public void expiredToken_rejected() {
        String token = token(reviewLinkService.issueLink(post));
        reviewLinkService.setClock(Clock.fixed(NOW.plus(Duration.ofHours(49)), ZoneOffset.UTC));
        assertFalse(reviewLinkService.resolve(token).isPresent());
    }

    @Test
    public void consumedToken_isSingleUse() {
        String token = token(reviewLinkService.issueLink(post));
        assertTrue(reviewLinkService.consume(token).isPresent());
        assertNull(post.getReviewLinkNonce());
        verify(postRepository).save(post);
        assertFalse(reviewLinkService.consume(token).isPresent());
    }

    @Test
    public void restore_makesConsumedTokenUsableAgain() {
        String token = token(reviewLinkService.issueLink(post));
        reviewLinkService.consume(token);
        reviewLinkService.restore(post, token);
        assertTrue(reviewLinkService.resolve(token).isPresent());
    }

    @Test
    public void newerLink_replacesOlderOne() {
        String first = token(reviewLinkService.issueLink(post));
        String second = token(reviewLinkService.issueLink(post));
        assertNotEquals(first, second);
        assertFalse(reviewLinkService.resolve(first).isPresent());
        assertTrue(reviewLinkService.resolve(second).isPresent());
    }

    @Test
    public void draftNoLongerPending_rejected() {
        String token = token(reviewLinkService.issueLink(post));
        post.setDraftStatus(DraftStatus.REJECTED);
        assertFalse(reviewLinkService.resolve(token).isPresent());
        post.setDraftStatus(DraftStatus.PENDING_REVIEW);
        post.setPostedStatus(true);
        assertFalse(reviewLinkService.resolve(token).isPresent());
    }

    @Test
    public void disabled_noLinkAndNothingResolves() {
        String token = token(reviewLinkService.issueLink(post));
        when(reviewLinkProperties.isEnabled()).thenReturn(false);
        assertNull(reviewLinkService.issueLink(post));
        assertFalse(reviewLinkService.resolve(token).isPresent());
    }

    @Test
    public void properties_requireBaseUrlAndLongSecret() {
        ReviewLinkProperties properties = new ReviewLinkProperties();
        assertFalse(properties.isEnabled());
        properties.setBaseUrl("https://updater.example.com");
        properties.setSecret("short");
        assertFalse(properties.isEnabled());
        properties.setSecret("test-secret-that-is-at-least-32-characters");
        assertTrue(properties.isEnabled());
    }
}
