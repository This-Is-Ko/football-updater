package com.ko.footballupdater.models.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacebookApiDto {

    private Boolean currentlyLoggedIn;

    private String loginUri;

}
