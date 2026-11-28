package com.ko.footballupdater.unit.photo;

import com.ko.footballupdater.configuration.PhotoProperties;
import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.Player;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.photo.CenterFramer;
import com.ko.footballupdater.photo.FaceMatcherSidecarClient;
import com.ko.footballupdater.photo.Framer;
import com.ko.footballupdater.photo.ImageFetchException;
import com.ko.footballupdater.photo.ImageFetcher;
import com.ko.footballupdater.photo.ManualUrlPhotoSource;
import com.ko.footballupdater.photo.NoOpPlayerMatcher;
import com.ko.footballupdater.photo.NoPhotoSource;
import com.ko.footballupdater.photo.PhotoCandidate;
import com.ko.footballupdater.photo.PhotoPipelineService;
import com.ko.footballupdater.photo.PhotoSource;
import com.ko.footballupdater.unit.llm.LlmTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PhotoPipelineServiceTest {

    /**
     * Stands in for a future automatic photo source.
     */
    static class FakeAutomaticSource implements PhotoSource {
        @Override
        public String name() {
            return "FAKE_AUTO";
        }

        @Override
        public List<PhotoCandidate> findCandidates(Player player, Match match) {
            return List.of(new PhotoCandidate("https://photos.example/auto.jpg", "FAKE_AUTO", "CC BY", null, null));
        }
    }

    private PhotoPipelineService service;
    private PhotoProperties photoProperties;
    private ImageFetcher imageFetcher;
    private Post post;

    @BeforeEach
    public void setup() throws Exception {
        service = new PhotoPipelineService();
        photoProperties = new PhotoProperties();
        imageFetcher = mock(ImageFetcher.class);
        when(imageFetcher.fetchImage(anyString())).thenReturn(new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB));
        ManualUrlPhotoSource manualSource = new ManualUrlPhotoSource();
        // Stands in for the VISION framer
        Framer visionFramer = new Framer() {
            @Override
            public String name() {
                return "VISION";
            }

            @Override
            public com.ko.footballupdater.models.form.ImageGenParams frame(BufferedImage image, int targetSize) {
                return new com.ko.footballupdater.models.form.ImageGenParams();
            }
        };
        ReflectionTestUtils.setField(service, "photoProperties", photoProperties);
        ReflectionTestUtils.setField(service, "photoSources", List.of(new NoPhotoSource(), manualSource, new FakeAutomaticSource()));
        ReflectionTestUtils.setField(service, "playerMatchers", List.of(new NoOpPlayerMatcher(), new FaceMatcherSidecarClient()));
        ReflectionTestUtils.setField(service, "framers", List.of(new CenterFramer(), visionFramer));
        ReflectionTestUtils.setField(service, "imageFetcher", imageFetcher);
        ReflectionTestUtils.setField(service, "manualUrlPhotoSource", manualSource);
        post = LlmTestData.post(1, "Jamie Rivers");
    }

    @Test
    public void defaults_noSourceNoopMatcherCenterFramer() {
        assertEquals(List.of(NoPhotoSource.NAME), service.activeSources().stream().map(PhotoSource::name).toList());
        assertEquals(NoOpPlayerMatcher.NAME, service.matcher().name());
        assertEquals(CenterFramer.NAME, service.framer().name());
        assertTrue(service.findAutomaticPhoto(post).isEmpty());
    }

    @Test
    public void config_priorityListAndNamesAreCaseInsensitive() {
        photoProperties.setSources(List.of(" manual_url", "UNKNOWN", "none"));
        photoProperties.setMatcher("face_sidecar");
        photoProperties.setFramer("vision");

        assertEquals(List.of(ManualUrlPhotoSource.NAME, NoPhotoSource.NAME), service.activeSources().stream().map(PhotoSource::name).toList());
        assertEquals(FaceMatcherSidecarClient.NAME, service.matcher().name());
        assertEquals("VISION", service.framer().name());
    }

    @Test
    public void config_unknownMatcherAndFramer_fallBackToSafeDefaults() {
        photoProperties.setMatcher("YOLO");
        photoProperties.setFramer("FANCY");

        assertEquals(NoOpPlayerMatcher.NAME, service.matcher().name());
        assertEquals(CenterFramer.NAME, service.framer().name());
    }

    @Test
    public void automaticPhoto_noopMatcher_neverUsed() throws Exception {
        photoProperties.setSources(List.of("FAKE_AUTO"));

        assertTrue(service.findAutomaticPhoto(post).isEmpty());
        // It was considered and rejected by the matcher
        verify(imageFetcher).fetchImage("https://photos.example/auto.jpg");
    }

    @Test
    public void automaticPhoto_faceSidecarStub_neverUsed() {
        photoProperties.setSources(List.of("FAKE_AUTO"));
        photoProperties.setMatcher(FaceMatcherSidecarClient.NAME);

        assertTrue(service.findAutomaticPhoto(post).isEmpty());
    }

    @Test
    public void manualPhoto_noopMatcher_passesAndIsFramed() throws Exception {
        PhotoPipelineService.SelectedPhoto photo = service.prepareManualPhoto(post, " https://photos.example/mine.jpg ", " Taken by me ");

        assertEquals("https://photos.example/mine.jpg", photo.candidate().url());
        assertEquals(ManualUrlPhotoSource.NAME, photo.candidate().sourceName());
        assertEquals("Taken by me", photo.candidate().licence());
        assertTrue(photo.matchResult().passed());
        assertEquals(500, photo.imageGenParams().getImageHorizontalOffset());
    }

    @Test
    public void manualPhoto_fetchFails_throws() throws Exception {
        when(imageFetcher.fetchImage(anyString())).thenThrow(new ImageFetchException("Image URL must use http or https"));

        assertThrows(ImageFetchException.class, () -> service.prepareManualPhoto(post, "file:///etc/passwd", null));
    }

    @Test
    public void manualPhoto_faceSidecarStub_rejected() {
        photoProperties.setMatcher(FaceMatcherSidecarClient.NAME);

        ImageFetchException ex = assertThrows(ImageFetchException.class,
                () -> service.prepareManualPhoto(post, "https://photos.example/mine.jpg", null));
        assertTrue(ex.getMessage().contains("not implemented"));
    }

    @Test
    public void noPhotoSource_returnsNothing() throws Exception {
        assertTrue(new NoPhotoSource().findCandidates(post.getPlayer(), null).isEmpty());
        verify(imageFetcher, never()).fetchImage(anyString());
    }
}
