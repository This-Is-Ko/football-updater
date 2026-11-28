package com.ko.footballupdater.photo;

import com.ko.footballupdater.models.form.HorizontalTranslation;
import com.ko.footballupdater.models.form.ImageGenParams;
import com.ko.footballupdater.models.form.VerticalTranslation;

/**
 * Converts a point of interest into ImageGenParams for the standout background in ImageGeneratorService.
 * With forceScaleImage the shorter side is scaled to the canvas, so the visible window is a square
 * with the side of the shorter edge, moved along the longer edge by the offset (in source pixels).
 */
public final class FramingMath {

    private FramingMath() {
    }

    /**
     * @param subjectTop top of the subject in source pixels, kept in view for portrait photos (null to ignore)
     */
    public static ImageGenParams frameAround(int width, int height, double centerX, double centerY, Double subjectTop) {
        ImageGenParams params = new ImageGenParams();
        params.setForceScaleImage(true);
        if (width == height) {
            params.setImageHorizontalTranslation(HorizontalTranslation.NONE);
            params.setImageVerticalTranslation(VerticalTranslation.NONE);
            return params;
        }
        if (width > height) {
            int window = height;
            int offset = clamp((int) Math.round(centerX - window / 2.0), 0, width - window);
            params.setImageHorizontalTranslation(HorizontalTranslation.LEFT);
            params.setImageHorizontalOffset(offset);
            return params;
        }
        int window = width;
        int offset = (int) Math.round(centerY - window / 2.0);
        if (subjectTop != null) {
            // Keep the top of the subject (usually the head) in the frame, with a small margin
            offset = Math.min(offset, (int) Math.round(subjectTop - window * 0.05));
        }
        params.setImageVerticalTranslation(VerticalTranslation.TOP);
        params.setImageVerticalOffset(clamp(offset, 0, height - window));
        return params;
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
