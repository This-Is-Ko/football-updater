package com.ko.footballupdater.unit.llm;

import com.ko.footballupdater.configuration.ImageGeneratorProperties;
import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.LlmException;
import com.ko.footballupdater.llm.VisionQaService;
import com.ko.footballupdater.llm.models.QaIssue;
import com.ko.footballupdater.llm.models.QaIssueType;
import com.ko.footballupdater.llm.models.QaVerdict;
import com.ko.footballupdater.models.Post;
import com.ko.footballupdater.photo.ImageFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VisionQaServiceTest {

    @TempDir
    Path outputDir;

    private VisionQaService visionQaService;
    private StubLlmClient llmClient;
    private ImageFetcher imageFetcher;
    private Post post;

    @BeforeEach
    public void setup() throws Exception {
        llmClient = new StubLlmClient();
        imageFetcher = mock(ImageFetcher.class);
        ImageGeneratorProperties imageGeneratorProperties = new ImageGeneratorProperties();
        imageGeneratorProperties.setOutputPath(outputDir.toString());
        visionQaService = new VisionQaService();
        ReflectionTestUtils.setField(visionQaService, "llmClient", llmClient);
        ReflectionTestUtils.setField(visionQaService, "llmProperties", new LlmProperties());
        ReflectionTestUtils.setField(visionQaService, "imageGeneratorProperties", imageGeneratorProperties);
        ReflectionTestUtils.setField(visionQaService, "imageFetcher", imageFetcher);

        // Two rendered images on disk, as after image generation
        for (String name : List.of("JamieRivers_standout_stat_image_1.jpg", "JamieRivers_stat_image_1.jpg")) {
            BufferedImage image = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.drawString("2 GOALS", 400, 900);
            graphics.dispose();
            ImageIO.write(image, "jpg", new File(outputDir.toFile(), name));
        }
        post = LlmTestData.post(1, "Jamie Rivers");
        post.setCaption("Jamie Rivers with 2 goals");
        post.setImagesUrls(new ArrayList<>(List.of(
                "https://bucket.s3.amazonaws.com/dev/generated-images/JamieRivers_standout_stat_image_1.jpg",
                "https://bucket.s3.amazonaws.com/dev/generated-images/JamieRivers_stat_image_1.jpg")));
    }

    @Test
    public void review_issuesFound_storedAsFlaggedWithoutChangingPost() throws Exception {
        llmClient.thenReturn(new QaVerdict(false, List.of(
                new QaIssue(QaIssueType.TEXT_OVERFLOW, "Image 2: player name runs off the right edge"),
                new QaIssue(null, "Something odd"))));
        List<String> imagesBefore = new ArrayList<>(post.getImagesUrls());

        visionQaService.review(post);

        assertFalse(post.getQaPassed());
        assertNotNull(post.getQaCheckedAt());
        List<QaIssue> issues = post.getQaIssueList();
        assertEquals(2, issues.size());
        assertEquals(QaIssueType.TEXT_OVERFLOW, issues.get(0).type());
        assertEquals(QaIssueType.OTHER, issues.get(1).type());
        // QA never edits the post
        assertEquals(imagesBefore, post.getImagesUrls());
        assertEquals("Jamie Rivers with 2 goals", post.getCaption());

        StubLlmClient.Call call = llmClient.calls.get(0);
        assertEquals("claude-opus-5-5", call.model());
        assertEquals(2, call.images().size());
        assertTrue(call.userPrompt().contains("Jamie Rivers with 2 goals"));
        assertTrue(call.userPrompt().contains("\"goals\" : 2"));
        // Images came from the local output folder, not downloaded
        verify(imageFetcher, never()).fetchImage(anyString());
    }

    @Test
    public void review_noIssues_passed() {
        llmClient.thenReturn(new QaVerdict(true, List.of()));

        visionQaService.review(post);

        assertTrue(post.getQaPassed());
        assertTrue(post.getQaIssueList().isEmpty());
    }

    @Test
    public void review_passTrueButIssuesListed_flagged() {
        llmClient.thenReturn(new QaVerdict(true, List.of(new QaIssue(QaIssueType.WATERMARK, "Logo in the corner"))));

        visionQaService.review(post);

        assertFalse(post.getQaPassed());
    }

    @Test
    public void review_imageNotOnDisk_downloaded() throws Exception {
        post.setImagesUrls(new ArrayList<>(List.of("https://bucket.s3.amazonaws.com/other.jpg")));
        when(imageFetcher.fetchImage("https://bucket.s3.amazonaws.com/other.jpg")).thenReturn(new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB));
        llmClient.thenReturn(new QaVerdict(true, List.of()));

        visionQaService.review(post);

        verify(imageFetcher).fetchImage("https://bucket.s3.amazonaws.com/other.jpg");
        assertTrue(post.getQaPassed());
    }

    @Test
    public void review_largeImagesDownscaledForRequest() throws Exception {
        post.setImagesUrls(new ArrayList<>(List.of("https://bucket.s3.amazonaws.com/huge.jpg")));
        when(imageFetcher.fetchImage(anyString())).thenReturn(new BufferedImage(4000, 3000, BufferedImage.TYPE_INT_RGB));
        llmClient.thenReturn(new QaVerdict(true, List.of()));

        visionQaService.review(post);

        BufferedImage sent = ImageIO.read(new java.io.ByteArrayInputStream(llmClient.calls.get(0).images().get(0).data()));
        assertEquals(1568, sent.getWidth());
    }

    @Test
    public void review_llmFails_verdictCleared() {
        post.setQaPassed(true);
        llmClient.thenThrow(new LlmException("overloaded"));

        visionQaService.review(post);

        assertNull(post.getQaPassed());
        assertNull(post.getQaIssues());
    }

    @Test
    public void review_llmDisabled_skipped() {
        llmClient.enabled(false);

        visionQaService.review(post);

        assertTrue(llmClient.calls.isEmpty());
        assertNull(post.getQaPassed());
    }

    @Test
    public void parseIssues_badJson_singleOtherIssue() {
        List<QaIssue> issues = VisionQaService.parseIssues("{not json");
        assertEquals(Arrays.asList(new QaIssue(QaIssueType.OTHER, "Unreadable QA result")), issues);
        assertTrue(VisionQaService.parseIssues(null).isEmpty());
    }
}
