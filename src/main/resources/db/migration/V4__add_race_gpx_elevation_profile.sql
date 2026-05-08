ALTER TABLE races
    ADD COLUMN gpx_file_name VARCHAR(255),
    ADD COLUMN gpx_imported_at TIMESTAMP,
    ADD COLUMN elevation_loss_m INTEGER,
    ADD COLUMN min_elevation_m INTEGER,
    ADD COLUMN max_elevation_m INTEGER;

ALTER TABLE races
    ADD CONSTRAINT chk_races_elevation_loss_non_negative
        CHECK (elevation_loss_m IS NULL OR elevation_loss_m >= 0),
    ADD CONSTRAINT chk_races_elevation_bounds_order
        CHECK (min_elevation_m IS NULL OR max_elevation_m IS NULL OR min_elevation_m <= max_elevation_m);
