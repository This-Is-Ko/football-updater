package com.ko.footballupdater.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Signed, single-use links in the update email that open a draft on a phone without the dashboard login.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "review-link")
public class ReviewLinkProperties {

    /**
     * Public address of this app, used to build the links, e.g. https://updater.example.com
     */
    private String baseUrl;

    /**
     * HMAC key the links are signed with. Links are only added to the email when this and baseUrl are set.
     * Use a long random value; changing it invalidates every link already sent.
     */
    private String secret;

    /**
     * How long a link stays valid.
     */
    private long expiryHours = 48;

    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.isBlank() && secret != null && secret.length() >= 32;
    }
}
