package com.ko.footballupdater.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the standout photo pipeline.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "photo")
public class PhotoProperties {

    /**
     * Photo sources in priority order, e.g. NONE or MANUAL_URL.
     */
    private List<String> sources = new ArrayList<>(List.of("NONE"));

    /**
     * NOOP (default, only reviewer-picked photos pass) or FACE_SIDECAR (step 4, not implemented yet).
     */
    private String matcher = "NOOP";

    /**
     * CENTER (default) or VISION.
     */
    private String framer = "CENTER";

    private long maxDownloadBytes = 10L * 1024 * 1024;

    private int downloadTimeoutSeconds = 10;

    /**
     * Largest image (width x height) that will be decoded, to avoid decompression bombs.
     */
    private long maxImagePixels = 40_000_000L;

    /**
     * Allow fetching from loopback, private and link-local addresses. Only for tests.
     */
    private boolean allowPrivateAddresses = false;
}
