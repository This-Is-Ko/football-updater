package com.ko.footballupdater.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FacebookApiHelper {

    public static String encodeTextToUsAscii(String text) {
        return URLEncoder.encode(text, StandardCharsets.US_ASCII);
    }

    public static String encodeTextToUtf8(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    public static String maskAccessToken(String url) {
        // Replace the access_token value with a placeholder
        return url.replaceAll("access_token=([^&]+)", "access_token=********");
    }
}
