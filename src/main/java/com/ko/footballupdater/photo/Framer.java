package com.ko.footballupdater.photo;

import com.ko.footballupdater.models.form.ImageGenParams;

import java.awt.image.BufferedImage;

/**
 * Works out how to crop a photo into the square standout canvas. Selected with photo.framer (PHOTO_FRAMER).
 */
public interface Framer {

    String name();

    /**
     * @param targetSize side of the square output canvas in pixels (the standout image uses 1000)
     * @return translation and offsets for ImageGeneratorService, offsets in source image pixels
     */
    ImageGenParams frame(BufferedImage image, int targetSize);
}
