package com.ko.footballupdater.unit.photo;

import com.ko.footballupdater.configuration.FaceMatchingProperties;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerFaceEmbedding;
import com.ko.footballupdater.photo.FaceMatcherSidecarClient;
import com.ko.footballupdater.photo.ManualUrlPhotoSource;
import com.ko.footballupdater.photo.MatchResult;
import com.ko.footballupdater.photo.PhotoCandidate;
import com.ko.footballupdater.photo.face.FaceSidecarClient;
import com.ko.footballupdater.photo.face.FaceSidecarException;
import com.ko.footballupdater.photo.face.FaceSidecarModels.MatchResponse;
import com.ko.footballupdater.photo.face.FaceSidecarModels.ScoredFace;
import com.ko.footballupdater.photo.face.IdentityCrossCheck;
import com.ko.footballupdater.repositories.PlayerFaceEmbeddingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FaceMatcherSidecarClientTest {

    private static final PhotoCandidate AUTO = new PhotoCandidate("https://photos.example/a.jpg", "FAKE_AUTO", "CC BY", "Rivers scores", null);
    private static final PhotoCandidate MANUAL = new PhotoCandidate("https://photos.example/m.jpg", ManualUrlPhotoSource.NAME, null, null, null);

    private FaceMatcherSidecarClient matcher;
    private FaceMatchingProperties properties;
    private FaceSidecarClient sidecar;
    private PlayerFaceEmbeddingRepository repository;
    private IdentityCrossCheck crossCheck;
    private Player player;
    private final BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);

    @BeforeEach
    public void setup() {
        matcher = new FaceMatcherSidecarClient();
        properties = new FaceMatchingProperties();
        sidecar = mock(FaceSidecarClient.class);
        repository = mock(PlayerFaceEmbeddingRepository.class);
        crossCheck = mock(IdentityCrossCheck.class);
        ReflectionTestUtils.setField(matcher, "properties", properties);
        ReflectionTestUtils.setField(matcher, "sidecarClient", sidecar);
        ReflectionTestUtils.setField(matcher, "embeddingRepository", repository);
        ReflectionTestUtils.setField(matcher, "identityCrossCheck", crossCheck);
        player = new Player("Jamie Rivers");
        player.setId(7);
        when(crossCheck.check(any(), any(), any())).thenReturn(new IdentityCrossCheck.Result(IdentityCrossCheck.Verdict.UNKNOWN, List.of("Nothing readable")));
    }

    private void enrol(int count) {
        List<PlayerFaceEmbedding> embeddings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            embeddings.add(new PlayerFaceEmbedding(7, "buffalo_l", new float[]{1, 0, 0}, "https://ref/" + i, 0.9));
        }
        when(repository.findByPlayerIdAndModel(7, "buffalo_l")).thenReturn(embeddings);
    }

    private static ScoredFace face(int index, double similarity) {
        return new ScoredFace(index, List.of(0.0, 0.0, 100.0, 100.0), 0.9, similarity, similarity);
    }

    private void sidecarReturns(ScoredFace subject, ScoredFace best, int faceCount) throws Exception {
        when(sidecar.match(any(), anyList())).thenReturn(new MatchResponse("buffalo_l", faceCount, subject, best, List.of()));
    }

    @Test
    public void auto_highSimilarity_passesUnflaggedConfidence() throws Exception {
        enrol(3);
        sidecarReturns(face(0, 0.62), face(0, 0.62), 1);

        MatchResult result = matcher.match(player, AUTO, image);

        assertTrue(result.passed());
        assertEquals(0.62, result.confidence(), 1e-9);
        assertTrue(result.reasons().get(0).contains("0.62"));
    }

    @Test
    public void auto_reviewBand_passesWithLowConfidence() throws Exception {
        enrol(3);
        sidecarReturns(face(0, 0.38), face(0, 0.38), 1);

        MatchResult result = matcher.match(player, AUTO, image);

        assertTrue(result.passed());
        assertTrue(result.confidence() < properties.getConfidentSimilarity());
    }

    @Test
    public void auto_belowMinimum_fails() throws Exception {
        enrol(3);
        sidecarReturns(face(0, 0.12), face(0, 0.12), 1);

        assertFalse(matcher.match(player, AUTO, image).passed());
        verify(crossCheck, never()).check(any(), any(), any());
    }

    @Test
    public void auto_sidecarDown_skipsPhoto() throws Exception {
        enrol(3);
        when(sidecar.match(any(), anyList())).thenThrow(new FaceSidecarException("Connection refused"));

        MatchResult result = matcher.match(player, AUTO, image);

        assertFalse(result.passed());
        assertTrue(result.reasons().get(0).contains("unavailable"));
    }

    @Test
    public void auto_tooFewReferences_failsWithoutCallingSidecar() throws Exception {
        enrol(2);

        assertFalse(matcher.match(player, AUTO, image).passed());
        verify(sidecar, never()).match(any(), anyList());
    }

    @Test
    public void auto_noFace_fails() throws Exception {
        enrol(3);
        sidecarReturns(null, null, 0);

        assertFalse(matcher.match(player, AUTO, image).passed());
    }

    @Test
    public void auto_playerIsNotTheMainSubject_fails() throws Exception {
        enrol(3);
        sidecarReturns(face(0, 0.10), face(1, 0.70), 2);

        MatchResult result = matcher.match(player, AUTO, image);

        assertFalse(result.passed());
        assertTrue(result.reasons().get(0).contains("main subject"));
    }

    @Test
    public void auto_crossCheckContradicts_fails() throws Exception {
        enrol(3);
        sidecarReturns(face(0, 0.70), face(0, 0.70), 1);
        when(crossCheck.check(any(), any(), any())).thenReturn(new IdentityCrossCheck.Result(IdentityCrossCheck.Verdict.CONTRADICTS,
                List.of("Jersey number 9 but Jamie Rivers wears 10")));

        MatchResult result = matcher.match(player, AUTO, image);

        assertFalse(result.passed());
        assertTrue(result.reasons().contains("Jersey number 9 but Jamie Rivers wears 10"));
    }

    @Test
    public void auto_crossCheckDisabled_notCalled() throws Exception {
        properties.setCrossCheckEnabled(false);
        enrol(3);
        sidecarReturns(face(0, 0.70), face(0, 0.70), 1);

        assertTrue(matcher.match(player, AUTO, image).passed());
        verify(crossCheck, never()).check(any(), any(), any());
    }

    @Test
    public void auto_wrongModel_fails() throws Exception {
        enrol(3);
        when(sidecar.match(any(), anyList())).thenReturn(new MatchResponse("antelopev2", 1, face(0, 0.9), face(0, 0.9), List.of()));

        assertFalse(matcher.match(player, AUTO, image).passed());
    }

    @Test
    public void manual_sidecarDown_passesButZeroConfidence() throws Exception {
        enrol(3);
        when(sidecar.match(any(), anyList())).thenThrow(new FaceSidecarException("timed out"));

        MatchResult result = matcher.match(player, MANUAL, image);

        assertTrue(result.passed());
        assertEquals(0, result.confidence());
    }

    @Test
    public void manual_lowSimilarity_passesWithLowConfidence() throws Exception {
        enrol(1);
        sidecarReturns(face(0, 0.05), face(0, 0.05), 1);

        MatchResult result = matcher.match(player, MANUAL, image);

        assertTrue(result.passed());
        assertEquals(0.05, result.confidence(), 1e-9);
    }

    @Test
    public void manual_crossCheckContradicts_zeroConfidence() throws Exception {
        enrol(3);
        sidecarReturns(face(0, 0.70), face(0, 0.70), 1);
        when(crossCheck.check(any(), any(), any())).thenReturn(new IdentityCrossCheck.Result(IdentityCrossCheck.Verdict.CONTRADICTS,
                List.of("Caption names a different player")));

        MatchResult result = matcher.match(player, MANUAL, image);

        assertTrue(result.passed());
        assertEquals(0, result.confidence());
    }
}
