package com.ko.footballupdater.unit.services;

import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.models.form.ImageGenParams;
import com.ko.footballupdater.photo.CenterFramer;
import com.ko.footballupdater.photo.FramingMath;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
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
        InstagramPostProperies instagramPostProperies = new InstagramPostProperies();
        instagramPostProperies.setAccountName("testaccount");
        ReflectionTestUtils.setField(service, "instagramPostProperies", instagramPostProperies);
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

    private static BufferedImage photo(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        // A red "subject" near the right edge
        graphics.setColor(Color.RED);
        graphics.fillRect(width - 400, 0, 300, height);
        graphics.dispose();
        return image;
    }

    @Test
    public void standout_withFramedPhoto_rendersSquareImageAroundSubject() throws Exception {
        BufferedImage landscape = photo(2400, 1200);
        // Subject centre x = 2150, window 1200 wide -> offset clamped to 1200
        ImageGenParams params = FramingMath.frameAround(2400, 1200, 2150, 600, null);

        boolean usedGeneric = imageGeneratorService.generateStandoutStatsImage(post, stats, params, false, landscape);

        assertFalse(usedGeneric);
        verify(imageGeneratorService, never()).loadPlayerStandoutBaseImage(post);
        BufferedImage rendered = ImageIO.read(new File(outputDir.toFile(), post.getImagesFileNames().get(0)));
        assertEquals(1000, rendered.getWidth());
        assertEquals(1000, rendered.getHeight());
        // The subject (red) is in the frame: source x 2000-2300 maps to output x 667-917
        Color atSubject = new Color(rendered.getRGB(800, 100));
        assertTrue(atSubject.getRed() > 150 && atSubject.getBlue() < 100, "expected red at subject, got " + atSubject);
    }

    @Test
    public void standout_withCenterFramedPortraitPhoto_rendersSquareImage() throws Exception {
        BufferedImage portrait = photo(900, 1600);
        ImageGenParams params = new CenterFramer().frame(portrait, 1000);

        imageGeneratorService.generateStandoutStatsImage(post, stats, params, false, portrait);

        BufferedImage rendered = ImageIO.read(new File(outputDir.toFile(), post.getImagesFileNames().get(0)));
        assertEquals(1000, rendered.getWidth());
        assertEquals(1000, rendered.getHeight());
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
