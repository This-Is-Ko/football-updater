package com.ko.footballupdater.photo.face;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * JSON shapes returned by the face sidecar.
 */
public final class FaceSidecarModels {

    private FaceSidecarModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbeddedFace(List<Double> bbox, double detScore, float[] embedding) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbedResponse(String model, List<EmbeddedFace> faces) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScoredFace(int index, List<Double> bbox, double detScore, double maxSimilarity, double meanSimilarity) {
    }

    /**
     * @param subject   the largest confident face, or null when no face was found
     * @param bestMatch the face most like the player's references
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MatchResponse(String model, int faceCount, ScoredFace subject, ScoredFace bestMatch, List<ScoredFace> faces) {
    }
}
