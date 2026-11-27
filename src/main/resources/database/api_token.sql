-- Persisted OAuth tokens for external platforms (Instagram, TikTok), one row per platform.
-- Run once against the application database (spring.jpa.hibernate.ddl-auto is not used in prod).
CREATE TABLE IF NOT EXISTS api_token (
    platform           VARCHAR(32) NOT NULL,
    access_token       TEXT        NOT NULL,
    expires_at         DATETIME(6) NULL,
    refresh_token      TEXT        NULL,
    refresh_expires_at DATETIME(6) NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (platform)
);
