package com.ko.footballupdater.models.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageGenParams {

    private String backgroundImageUrl;

    private Boolean forceScaleImage = true;

    private HorizontalTranslation imageHorizontalTranslation = HorizontalTranslation.NONE;

    private Integer imageHorizontalOffset = 0;

}
