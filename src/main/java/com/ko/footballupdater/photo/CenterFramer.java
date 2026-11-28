package com.ko.footballupdater.photo;

import com.ko.footballupdater.models.form.ImageGenParams;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

/**
 * Default framer: crops the centre square of the photo.
 */
@Component
public class CenterFramer implements Framer {

    public static final String NAME = "CENTER";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ImageGenParams frame(BufferedImage image, int targetSize) {
        return FramingMath.frameAround(image.getWidth(), image.getHeight(), image.getWidth() / 2.0, image.getHeight() / 2.0, null);
    }
}
