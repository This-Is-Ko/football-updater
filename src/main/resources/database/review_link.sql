-- Nonce for the signed, single-use phone review links. Run once against the application database.
ALTER TABLE posts
    ADD COLUMN review_link_nonce VARCHAR(64) NULL;
