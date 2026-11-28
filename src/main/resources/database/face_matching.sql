-- Face matching (photo.matcher=FACE_SIDECAR). Run once against the application database.
-- Embeddings are biometric data: they are deleted with the player and replaced on re-enrolment.
CREATE TABLE player_face_embeddings
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id  INT           NOT NULL,
    model      VARCHAR(64)   NOT NULL,
    embedding  VARBINARY(4096) NOT NULL,
    source_url VARCHAR(2000) NULL,
    det_score  DOUBLE        NULL,
    created_at DATETIME(6)   NOT NULL,
    CONSTRAINT fk_face_embeddings_player FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE CASCADE
);

CREATE INDEX idx_face_embeddings_player ON player_face_embeddings (player_id, model);

ALTER TABLE players
    ADD COLUMN shirt_number INT NULL;

ALTER TABLE posts
    ADD COLUMN photo_match_confidence DOUBLE        NULL,
    ADD COLUMN photo_match_notes      VARCHAR(1000) NULL,
    ADD COLUMN photo_match_flagged    BIT           NULL;
