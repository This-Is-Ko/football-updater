package com.ko.footballupdater.models.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageGenParams {

    private String imageUrl;

    private Boolean forceScaleImage = true;

    private HorizontalTranslation imageHorizontalTranslation = HorizontalTranslation.NONE;

    private Integer imageHorizontalOffset = 0;

    private VerticalTranslation imageVerticalTranslation = VerticalTranslation.NONE;

    private Integer imageVerticalOffset = 0;

}
