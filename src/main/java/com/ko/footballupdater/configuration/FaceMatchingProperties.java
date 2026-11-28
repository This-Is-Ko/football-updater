package com.ko.footballupdater.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the face matcher sidecar (photo.matcher=FACE_SIDECAR).
 * Similarities are ArcFace cosine similarities; tune the thresholds on past posts.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "photo.face")
public class FaceMatchingProperties {

    /**
     * Base URL of the face sidecar, e.g. http://face-sidecar:8001.
     */
    private String sidecarUrl = "http://localhost:8001";

    /**
     * Face model pack the sidecar runs. Embeddings are stored per model.
     */
    private String model = "buffalo_l";

    private int connectTimeoutMillis = 2000;

    /**
     * Time allowed for the sidecar to answer. If it is slower, the photo is skipped.
     */
    private int readTimeoutMillis = 15000;

    /**
     * Below this similarity an automatic photo is rejected.
     */
    private double minSimilarity = 0.30;

    /**
     * Below this similarity a photo that passed is flagged on the approval card.
     */
    private double confidentSimilarity = 0.45;

    /**
     * Reference photos needed before automatic photos are matched, and the most kept per player.
     */
    private int minReferences = 3;

    private int maxReferences = 5;

    /**
     * Ask Claude (vision) to read the jersey number, the name on the shirt and the source caption.
     * Only used when the LLM is enabled.
     */
    private boolean crossCheckEnabled = true;
}
