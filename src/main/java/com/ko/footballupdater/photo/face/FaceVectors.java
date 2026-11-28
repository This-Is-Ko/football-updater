package com.ko.footballupdater.photo.face;

import java.util.List;

/**
 * Small vector helpers for face embeddings.
 */
public final class FaceVectors {

    private FaceVectors() {
    }

    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length || a.length == 0) {
            throw new IllegalArgumentException("Embeddings must be the same non-zero length");
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static double area(List<Double> bbox) {
        if (bbox == null || bbox.size() < 4) {
            return 0;
        }
        return Math.max(0, bbox.get(2) - bbox.get(0)) * Math.max(0, bbox.get(3) - bbox.get(1));
    }
}
