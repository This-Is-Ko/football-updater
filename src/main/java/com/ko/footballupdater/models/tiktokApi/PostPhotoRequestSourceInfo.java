package com.ko.footballupdater.models.tiktokApi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostPhotoRequestSourceInfo {

    private String source;

    @JsonProperty("photo_cover_index")
    private int photoCoverIndex;

    @JsonProperty("photo_images")
    private List<String> photoImages;

}
