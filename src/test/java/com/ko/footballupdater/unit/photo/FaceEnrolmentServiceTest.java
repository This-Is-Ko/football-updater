package com.ko.footballupdater.unit.photo;

import com.ko.footballupdater.configuration.FaceMatchingProperties;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.PlayerFaceEmbedding;
import com.ko.footballupdater.photo.ImageFetchException;
import com.ko.footballupdater.photo.ImageFetcher;
import com.ko.footballupdater.photo.face.FaceEnrolmentService;
import com.ko.footballupdater.photo.face.FaceSidecarClient;
import com.ko.footballupdater.photo.face.FaceSidecarException;
import com.ko.footballupdater.photo.face.FaceSidecarModels.EmbedResponse;
import com.ko.footballupdater.photo.face.FaceSidecarModels.EmbeddedFace;
import com.ko.footballupdater.repositories.PlayerFaceEmbeddingRepository;
import com.ko.footballupdater.repositories.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FaceEnrolmentServiceTest {

    private static final float[] PLAYER = {1, 0, 0};
    private static final float[] PLAYER_2 = {0.95f, 0.3f, 0};
    private static final float[] OTHER = {0, 0, 1};

    private FaceEnrolmentService service;
    private FaceSidecarClient sidecar;
    private PlayerFaceEmbeddingRepository repository;
    private ImageFetcher imageFetcher;

    @BeforeEach
    public void setup() throws Exception {
        service = new FaceEnrolmentService();
        sidecar = mock(FaceSidecarClient.class);
        repository = mock(PlayerFaceEmbeddingRepository.class);
        imageFetcher = mock(ImageFetcher.class);
        PlayerRepository playerRepository = mock(PlayerRepository.class);
        when(playerRepository.findById(7)).thenReturn(Optional.of(new Player("Jamie Rivers")));
        when(imageFetcher.fetchImage(any())).thenReturn(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB));
        ReflectionTestUtils.setField(service, "sidecarClient", sidecar);
        ReflectionTestUtils.setField(service, "properties", new FaceMatchingProperties());
        ReflectionTestUtils.setField(service, "embeddingRepository", repository);
        ReflectionTestUtils.setField(service, "playerRepository", playerRepository);
        ReflectionTestUtils.setField(service, "imageFetcher", imageFetcher);
    }

    private static EmbeddedFace face(double size, float[] embedding) {
        return new EmbeddedFace(List.of(0.0, 0.0, size, size), 0.9, embedding);
    }

    private static EmbedResponse faces(EmbeddedFace... faces) {
        return new EmbedResponse("buffalo_l", List.of(faces));
    }

    @Test
    public void enrol_threeGoodPhotos_replacesEmbeddings() throws Exception {
        when(sidecar.embed(any())).thenReturn(faces(face(100, PLAYER)), faces(face(100, PLAYER_2)), faces(face(100, PLAYER), face(10, OTHER)));

        FaceEnrolmentService.EnrolmentResult result = service.enrol(7, List.of("https://r/1.jpg", "https://r/2.jpg", "https://r/3.jpg", "https://r/1.jpg"));

        assertEquals(3, result.enrolled());
        assertTrue(result.readyForMatching());
        assertTrue(result.rejected().isEmpty());
        verify(repository).deleteByPlayerId(7);
        ArgumentCaptor<PlayerFaceEmbedding> saved = ArgumentCaptor.forClass(PlayerFaceEmbedding.class);
        verify(repository, times(3)).save(saved.capture());
        assertArrayEquals(PLAYER, saved.getAllValues().get(0).embeddingValues());
        assertEquals("buffalo_l", saved.getAllValues().get(0).getModel());
    }

    @Test
    public void enrol_rejectsNoFaceSeveralFacesAndOddOneOut() throws Exception {
        when(sidecar.embed(any())).thenReturn(
                faces(),
                faces(face(100, PLAYER), face(90, OTHER)),
                faces(face(100, PLAYER)),
                faces(face(100, PLAYER_2)),
                faces(face(100, OTHER)));

        FaceEnrolmentService.EnrolmentResult result = service.enrol(7,
                List.of("https://r/none.jpg", "https://r/group.jpg", "https://r/a.jpg", "https://r/b.jpg", "https://r/someone-else.jpg"));

        assertEquals(2, result.enrolled());
        assertFalse(result.readyForMatching());
        assertEquals(3, result.rejected().size());
        assertEquals("https://r/someone-else.jpg", result.rejected().get(2).url());
    }

    @Test
    public void enrol_downloadFails_photoRejected() throws Exception {
        when(imageFetcher.fetchImage("https://r/bad.jpg")).thenThrow(new ImageFetchException("HTTP 404"));
        when(sidecar.embed(any())).thenReturn(faces(face(100, PLAYER)));

        FaceEnrolmentService.EnrolmentResult result = service.enrol(7, List.of("https://r/bad.jpg", "https://r/ok.jpg"));

        assertEquals(1, result.enrolled());
        assertTrue(result.rejected().get(0).reason().contains("404"));
    }

    @Test
    public void enrol_sidecarDown_changesNothing() throws Exception {
        when(sidecar.embed(any())).thenThrow(new FaceSidecarException("down"));

        assertThrows(FaceSidecarException.class, () -> service.enrol(7, List.of("https://r/1.jpg")));
        verify(repository, never()).deleteByPlayerId(anyInt());
    }

    @Test
    public void enrol_nothingAccepted_keepsExisting() throws Exception {
        when(sidecar.embed(any())).thenReturn(faces());

        service.enrol(7, List.of("https://r/1.jpg"));

        verify(repository, never()).deleteByPlayerId(anyInt());
        verify(repository, never()).save(any());
    }

    @Test
    public void enrol_unknownPlayerOrNoUrls_rejected() {
        assertThrows(IllegalArgumentException.class, () -> service.enrol(99, List.of("https://r/1.jpg")));
        assertThrows(IllegalArgumentException.class, () -> service.enrol(7, List.of(" ")));
    }

    @Test
    public void embeddingBytes_roundTrip() {
        float[] values = {0.1f, -2.5f, 3e-7f};
        assertArrayEquals(values, PlayerFaceEmbedding.fromBytes(PlayerFaceEmbedding.toBytes(values)));
    }
}
