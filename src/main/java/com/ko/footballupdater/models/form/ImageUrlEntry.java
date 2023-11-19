package com.ko.footballupdater.models.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageUrlEntry {

    private Integer imageIndex;

    private String url;

    public ImageUrlEntry() {
    }

    public ImageUrlEntry(Integer imageIndex, String url) {
        this.imageIndex = imageIndex;
        this.url = url;
    }
}
