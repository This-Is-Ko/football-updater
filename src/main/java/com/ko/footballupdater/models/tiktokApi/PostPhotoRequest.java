package com.ko.footballupdater.models.tiktokApi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostPhotoRequest {

    @JsonProperty("post_info")
    private PostPhotoRequestPostInfo postInfo;

    @JsonProperty("source_info")
    private PostPhotoRequestSourceInfo sourceInfo;

    @JsonProperty("post_mode")
    private String postMode;

    @JsonProperty("media_type")
    private String mediaType;

}
