CREATE TABLE race_gpx_files (
    race_id BIGINT PRIMARY KEY REFERENCES races(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    sha256 VARCHAR(64),
    content BYTEA NOT NULL,
    imported_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_race_gpx_files_sha256_hex
        CHECK (sha256 IS NULL OR sha256 ~ '^[0-9a-f]{64}$')
);
