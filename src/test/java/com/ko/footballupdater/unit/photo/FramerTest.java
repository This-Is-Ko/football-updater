package com.ko.footballupdater.unit.photo;

import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.LlmException;
import com.ko.footballupdater.models.form.HorizontalTranslation;
import com.ko.footballupdater.models.form.ImageGenParams;
import com.ko.footballupdater.models.form.VerticalTranslation;
import com.ko.footballupdater.photo.CenterFramer;
import com.ko.footballupdater.photo.FramingMath;
import com.ko.footballupdater.photo.SubjectBoxResponse;
import com.ko.footballupdater.photo.VisionFramer;
import com.ko.footballupdater.unit.llm.StubLlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FramerTest {

    private final CenterFramer centerFramer = new CenterFramer();
    private VisionFramer visionFramer;
    private StubLlmClient llmClient;

    @BeforeEach
    public void setup() {
        llmClient = new StubLlmClient();
        LlmProperties llmProperties = new LlmProperties();
        visionFramer = new VisionFramer();
        ReflectionTestUtils.setField(visionFramer, "llmClient", llmClient);
        ReflectionTestUtils.setField(visionFramer, "llmProperties", llmProperties);
        ReflectionTestUtils.setField(visionFramer, "centerFramer", centerFramer);
    }

    private static BufferedImage image(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    public void center_landscape_cropsMiddleSquare() {
        ImageGenParams params = centerFramer.frame(image(2000, 1000), 1000);

        assertEquals(HorizontalTranslation.LEFT, params.getImageHorizontalTranslation());
        assertEquals(500, params.getImageHorizontalOffset());
        assertTrue(params.getForceScaleImage());
    }

    @Test
    public void center_portrait_cropsMiddleSquare() {
        ImageGenParams params = centerFramer.frame(image(1000, 1600), 1000);

        assertEquals(VerticalTranslation.TOP, params.getImageVerticalTranslation());
        assertEquals(300, params.getImageVerticalOffset());
    }

    @Test
    public void center_square_noTranslation() {
        ImageGenParams params = centerFramer.frame(image(800, 800), 1000);

        assertEquals(HorizontalTranslation.NONE, params.getImageHorizontalTranslation());
        assertEquals(VerticalTranslation.NONE, params.getImageVerticalTranslation());
    }

    @Test
    public void framingMath_offsetsClampedToImageBounds() {
        assertEquals(0, FramingMath.frameAround(2000, 1000, 100, 500, null).getImageHorizontalOffset());
        assertEquals(1000, FramingMath.frameAround(2000, 1000, 1990, 500, null).getImageHorizontalOffset());
        assertEquals(0, FramingMath.frameAround(1000, 2000, 500, 50, null).getImageVerticalOffset());
        assertEquals(1000, FramingMath.frameAround(1000, 2000, 500, 1990, null).getImageVerticalOffset());
    }

    @Test
    public void framingMath_portraitKeepsTopOfSubjectInFrame() {
        // Centre of a tall subject would put the window at 900, cutting off the head at 400
        ImageGenParams params = FramingMath.frameAround(1000, 3000, 500, 1400, 400.0);

        assertEquals(350, params.getImageVerticalOffset());
    }

    @Test
    public void vision_validBox_framesAroundSubject() {
        // Subject on the right of a 2000x1000 photo, centre x = 1600
        llmClient.thenReturn(new SubjectBoxResponse(true, 0.7, 0.1, 0.9, 0.9));

        ImageGenParams params = visionFramer.frame(image(2000, 1000), 1000);

        assertEquals(HorizontalTranslation.LEFT, params.getImageHorizontalTranslation());
        assertEquals(1000, params.getImageHorizontalOffset());
        StubLlmClient.Call call = llmClient.calls.get(0);
        assertEquals("claude-opus-5-5", call.model());
        assertEquals(1, call.images().size());
        assertEquals("image/jpeg", call.images().get(0).mediaType());
        assertEquals(SubjectBoxResponse.class, call.responseType());
    }

    @Test
    public void vision_subjectLeftOfCentre_offsetFollowsSubject() {
        llmClient.thenReturn(new SubjectBoxResponse(true, 0.3, 0.1, 0.5, 0.9));

        // centre x = 800, window 1000 wide, offset 300
        assertEquals(300, visionFramer.frame(image(2000, 1000), 1000).getImageHorizontalOffset());
    }

    @Test
    public void vision_invalidBoxes_fallBackToCenter() {
        llmClient.thenReturn(new SubjectBoxResponse(false, 0.1, 0.1, 0.2, 0.2))
                .thenReturn(new SubjectBoxResponse(true, 0.8, 0.1, 0.2, 0.9))
                .thenReturn(new SubjectBoxResponse(true, -0.5, 0.1, 1.5, 0.9))
                .thenReturn(new SubjectBoxResponse(true, null, 0.1, 0.5, 0.9));

        for (int i = 0; i < 4; i++) {
            assertEquals(500, visionFramer.frame(image(2000, 1000), 1000).getImageHorizontalOffset());
        }
    }

    @Test
    public void vision_llmFails_fallsBackToCenter() {
        llmClient.thenThrow(new LlmException("overloaded"));

        assertEquals(500, visionFramer.frame(image(2000, 1000), 1000).getImageHorizontalOffset());
    }

    @Test
    public void vision_llmDisabled_centersWithoutCall() {
        llmClient.enabled(false);

        assertEquals(500, visionFramer.frame(image(2000, 1000), 1000).getImageHorizontalOffset());
        assertTrue(llmClient.calls.isEmpty());
    }
}
