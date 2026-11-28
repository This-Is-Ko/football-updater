package com.ko.footballupdater.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

/**
 * A face embedding from one reference photo of a player, used by the face matcher.
 * Embeddings are biometric data: only enrolled squad players have them, and they are deleted with the player
 * (ON DELETE CASCADE) or when the player is re-enrolled.
 */
@Getter
@Setter
@Entity
@Table(name = "player_face_embeddings")
public class PlayerFaceEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Integer playerId;

    /**
     * Face model that produced the embedding (e.g. buffalo_l). Embeddings from different models can't be compared.
     */
    @Column(name = "model", nullable = false, length = 64)
    private String model;

    /**
     * float32 values, little endian.
     */
    @Column(name = "embedding", nullable = false, length = 4096)
    private byte[] embedding;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(name = "det_score")
    private Double detScore;

    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    public PlayerFaceEmbedding() {
    }

    public PlayerFaceEmbedding(Integer playerId, String model, float[] embedding, String sourceUrl, Double detScore) {
        this.playerId = playerId;
        this.model = model;
        this.embedding = toBytes(embedding);
        this.sourceUrl = sourceUrl;
        this.detScore = detScore;
        this.createdAt = new Date();
    }

    public float[] embeddingValues() {
        return fromBytes(embedding);
    }

    public static byte[] toBytes(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : values) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }

    public static float[] fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] values = new float[bytes.length / Float.BYTES];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.getFloat();
        }
        return values;
    }
}
