package com.ko.footballupdater.models.tiktokApi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostPhotoRequestPostInfo {

    private String title;

    private String description;

    @JsonProperty("disable_comment")
    private boolean disableComment;

    @JsonProperty("privacy_level")
    private String privacyLevel;

    @JsonProperty("auto_add_music")
    private boolean autoAddMusic;

}
