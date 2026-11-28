package com.ko.footballupdater.photo;

import com.ko.footballupdater.llm.LlmClient;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Prepares images for vision requests: downscaled to keep token use down and encoded as JPEG.
 */
public final class VisionImages {

    public static final int MAX_EDGE = 1568;

    private VisionImages() {
    }

    public static LlmClient.LlmImage toJpeg(BufferedImage image) throws IOException {
        BufferedImage scaled = downscale(image, MAX_EDGE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(scaled, "jpg", out)) {
            throw new IOException("No JPEG writer available");
        }
        return new LlmClient.LlmImage(out.toByteArray(), "image/jpeg");
    }

    /**
     * Scale so the longest edge is at most maxEdge, converting to RGB (JPEG has no alpha).
     */
    public static BufferedImage downscale(BufferedImage image, int maxEdge) {
        double scale = Math.min(1.0, (double) maxEdge / Math.max(image.getWidth(), image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return result;
    }
}
