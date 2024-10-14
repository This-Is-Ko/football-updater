package com.ko.footballupdater.models.tiktokApi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatorInfoResponseData {

    @JsonProperty("creator_avatar_url")
    private String creatorAvatarUrl;

    @JsonProperty("creator_username")
    private String creatorUsername;

    @JsonProperty("creator_nickname")
    private String creatorNickname;

    @JsonProperty("privacy_level_options")
    private List<String> privacyLevelOptions;

    @JsonProperty("comment_disabled")
    private boolean commentDisabled;

    @JsonProperty("duet_disabled")
    private boolean duetDisabled;

    @JsonProperty("stitch_disabled")
    private boolean stitchDisabled;

    @JsonProperty("max_video_post_duration_sec")
    private int maxVideoPostDurationSec;

}
