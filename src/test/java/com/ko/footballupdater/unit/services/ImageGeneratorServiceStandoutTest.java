package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.models.form.StatisticEntryGenerateDto;
import com.ko.footballupdater.services.ImageGeneratorService;
import com.ko.footballupdater.unit.llm.LlmTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ImageGeneratorServiceStandoutTest {

    @TempDir
    Path outputDir;

    private ImageGeneratorService imageGeneratorService;
    private Post post;
    private final List<StatisticEntryGenerateDto> stats = List.of(new StatisticEntryGenerateDto("goals", "2", true));

    @BeforeEach
    public void setup() {
        ImageGeneratorProperties properties = new ImageGeneratorProperties();
        properties.setEnabled(true);
        properties.setOutputPath(outputDir.toString());
        properties.setInputPath("/input");
        properties.setGenericBaseImageFile("Generic_base_player_stat_image.jpg");
        ImageGeneratorService service = new ImageGeneratorService();
        ReflectionTestUtils.setField(service, "imageGeneratorProperties", properties);
        imageGeneratorService = spy(service);

        post = LlmTestData.post(1, "Jamie Rivers");
        post.setImagesFileNames(new ArrayList<>());
    }

    private static BufferedImage baseImage() {
        return new BufferedImage(1080, 1080, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    public void standout_playerBaseImageExists_usesPlayerImage() throws Exception {
        doReturn(baseImage()).when(imageGeneratorService).loadPlayerStandoutBaseImage(post);

        boolean usedGeneric = imageGeneratorService.generateStandoutStatsImage(post, stats, null, true);

        assertFalse(usedGeneric);
        verify(imageGeneratorService, never()).loadGenericBaseImage();
        assertEquals(1, post.getImagesFileNames().size());
        assertTrue(new File(outputDir.toFile(), post.getImagesFileNames().get(0)).exists());
    }

    @Test
    public void standout_playerBaseImageMissing_fallsBackToGenericBase() throws Exception {
        doThrow(new IOException("Can't read input file!")).when(imageGeneratorService).loadPlayerStandoutBaseImage(post);
        doReturn(baseImage()).when(imageGeneratorService).loadGenericBaseImage();

        boolean usedGeneric = imageGeneratorService.generateStandoutStatsImage(post, stats, null, true);

        assertTrue(usedGeneric);
        assertEquals(1, post.getImagesFileNames().size());
        assertTrue(new File(outputDir.toFile(), post.getImagesFileNames().get(0)).exists());
    }

    @Test
    public void standout_playerBaseImageUnreadable_fallsBackToGenericBase() throws Exception {
        doReturn(null).when(imageGeneratorService).loadPlayerStandoutBaseImage(post);
        doReturn(baseImage()).when(imageGeneratorService).loadGenericBaseImage();

        assertTrue(imageGeneratorService.generateStandoutStatsImage(post, stats, null, true));
    }

    @Test
    public void standout_bothBaseImagesMissing_throws() throws Exception {
        doThrow(new IOException("Can't read input file!")).when(imageGeneratorService).loadPlayerStandoutBaseImage(post);
        doThrow(new IOException("Generic base image is missing")).when(imageGeneratorService).loadGenericBaseImage();

        assertThrows(Exception.class, () -> imageGeneratorService.generateStandoutStatsImage(post, stats, null, true));
        assertTrue(post.getImagesFileNames().isEmpty());
    }

    @Test
    public void standout_fallbackNotAllowed_existingBehaviourThrows() throws Exception {
        doThrow(new IOException("Can't read input file!")).when(imageGeneratorService).loadPlayerStandoutBaseImage(post);

        // The manual standout flow keeps using the 3-arg method, which doesn't fall back
        assertThrows(Exception.class, () -> imageGeneratorService.generateStandoutStatsImage(post, stats, null));
        verify(imageGeneratorService, never()).loadGenericBaseImage();
    }
}
