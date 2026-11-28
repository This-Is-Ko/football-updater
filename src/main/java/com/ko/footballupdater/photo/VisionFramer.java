package com.ko.footballupdater.photo;

import com.ko.footballupdater.configuration.LlmProperties;
import com.ko.footballupdater.llm.LlmClient;
import com.ko.footballupdater.models.form.ImageGenParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Asks the vision model where the main subject is and frames around it.
 * Falls back to {@link CenterFramer} when the LLM is disabled or returns anything invalid.
 */
@Slf4j
@Component
public class VisionFramer implements Framer {

    public static final String NAME = "VISION";

    static final String SYSTEM_PROMPT = """
            You locate the main subject in football photos so they can be cropped to a square.
            The main subject is the most prominent footballer, usually in focus and nearest the camera.
            Return the bounding box around their whole visible body as fractions of the image width and height
            (0 to 1, origin top left). Set subjectFound to false if there is no clear main subject.
            """;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private CenterFramer centerFramer;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ImageGenParams frame(BufferedImage image, int targetSize) {
        if (!llmClient.isEnabled()) {
            return centerFramer.frame(image, targetSize);
        }
        try {
            SubjectBoxResponse box = llmClient.generateStructured(llmProperties.getVisionModel(), SYSTEM_PROMPT,
                    "Find the main subject in this photo.", List.of(VisionImages.toJpeg(image)), SubjectBoxResponse.class);
            if (!isValid(box)) {
                log.atInfo().setMessage("Vision framer returned no valid subject box, centering instead").log();
                return centerFramer.frame(image, targetSize);
            }
            double xMin = box.xMin() * image.getWidth();
            double xMax = box.xMax() * image.getWidth();
            double yMin = box.yMin() * image.getHeight();
            double yMax = box.yMax() * image.getHeight();
            return FramingMath.frameAround(image.getWidth(), image.getHeight(), (xMin + xMax) / 2, (yMin + yMax) / 2, yMin);
        } catch (Exception ex) {
            log.atWarn().setMessage("Vision framer failed, centering instead").setCause(ex).log();
            return centerFramer.frame(image, targetSize);
        }
    }

    static boolean isValid(SubjectBoxResponse box) {
        if (box == null || !Boolean.TRUE.equals(box.subjectFound())
                || box.xMin() == null || box.xMax() == null || box.yMin() == null || box.yMax() == null) {
            return false;
        }
        return inRange(box.xMin()) && inRange(box.xMax()) && inRange(box.yMin()) && inRange(box.yMax())
                && box.xMin() < box.xMax() && box.yMin() < box.yMax();
    }

    private static boolean inRange(double value) {
        return !Double.isNaN(value) && value >= 0 && value <= 1;
    }
}
