package com.ko.footballupdater.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class StringHelper {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
    private static final SecureRandom random = new SecureRandom();

    public static String generateRandomString(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            result.append(CHARACTERS.charAt(index));
        }
        return result.toString();
    }

    public static String encodeTextToUsAscii(String text) {
        return URLEncoder.encode(text, StandardCharsets.US_ASCII);
    }

    public static String encodeTextToUtf8(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    public static String encodeWithSHA256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);  // Encode to Base64 for readability
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String maskAccessToken(String url) {
        // Replace the access_token value with a placeholder
        return url.replaceAll("access_token=([^&]+)", "access_token=********");
    }
}
