package com.ko.footballupdater.utils;

import lombok.Getter;

@Getter
public enum MessageType {
    SUCCESS("success"),
    ERROR("error");

    private final String type;

    MessageType(String type) {
        this.type = type;
    }
}
