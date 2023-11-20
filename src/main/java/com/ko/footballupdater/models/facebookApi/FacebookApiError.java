package com.ko.footballupdater.models.facebookApi;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacebookApiError {

    private String message;
    private String type;
    private Integer code;
    private String fbtrace_id;

    public FacebookApiError(String message, String type, Integer code, String fbtrace_id) {
        this.message = message;
        this.type = type;
        this.code = code;
        this.fbtrace_id = fbtrace_id;
    }
}