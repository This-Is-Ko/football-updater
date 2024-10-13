package com.ko.footballupdater.models.tiktokApi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostPhotoResponseData {

    @JsonProperty("publish_id")
    private String publishId;
}
