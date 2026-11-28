-- Vision QA verdict and standout photo source for posts (photo pipeline). Run once against the application database.
ALTER TABLE posts
    ADD COLUMN qa_passed         BIT           NULL,
    ADD COLUMN qa_issues         VARCHAR(4000) NULL,
    ADD COLUMN qa_checked_at     DATETIME(6)   NULL,
    ADD COLUMN photo_source_url  VARCHAR(2000) NULL,
    ADD COLUMN photo_source_name VARCHAR(64)   NULL,
    ADD COLUMN photo_licence     VARCHAR(500)  NULL;
