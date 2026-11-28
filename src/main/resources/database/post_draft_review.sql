-- Approval queue fields for posts (editor/caption agents). Run once against the application database.
-- Existing posts keep a NULL draft_status and don't appear in the review queue.
ALTER TABLE posts
    ADD COLUMN draft_status   VARCHAR(32)   NULL,
    ADD COLUMN llm_reason     VARCHAR(2000) NULL,
    ADD COLUMN featured_stats VARCHAR(1000) NULL,
    ADD COLUMN caption_source VARCHAR(16)   NULL;

CREATE INDEX idx_posts_draft_status ON posts (draft_status, date_generated);
