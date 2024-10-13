package com.ko.footballupdater.models.tiktokApi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatorInfoResponse {

    private CreatorInfoResponseData data;
    private CreatorInfoResponseError error;

}
