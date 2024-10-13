package com.ko.footballupdater.models.tiktokApi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ko.footballupdater.models.facebookApi.AccessTokenResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TiktokAccessTokenResponse extends AccessTokenResponse {

    private String error;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("log_id")
    private String logId;

}
